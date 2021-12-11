package com.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.CartInfo;
import com.gmall.bean.SkuInfo;
import com.gmall.cart.consts.CartConst;
import com.gmall.cart.mapper.CartInfoMapper;
import com.gmall.manage.util.RedisUtil;
import com.gmall.service.CartService;
import com.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<CartInfo> getCheckedCartList(String userId) {
        /*
            1. 获取jedis
            2. 根据key获取数据并返回
         */

        Jedis jedis = null;
        List<CartInfo> checkedCartList = null;
        try {
            // 获取jedis
            jedis = redisUtil.getJedis();
            // 定义key
            String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
            // 根据key获取值
            List<String> jsonStringList = jedis.hvals(cartCheckedKey);
            // 将json串解析为CartInfo对象
            if (jsonStringList != null && jsonStringList.size() > 0) {
                checkedCartList = new ArrayList<>();
                for (String s : jsonStringList) {
                    checkedCartList.add(JSON.parseObject(s, CartInfo.class));
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        // 返回结果
        return checkedCartList;
    }

    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        /*
            更新缓存中SKU的勾选状态
         */
        String cartkey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            // 获取缓存中的记录
            String cartInfoJson = jedis.hget(cartkey, skuId);
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            // 更新
            cartInfo.setIsChecked(isChecked);
            String cartJsonStr = JSON.toJSONString(cartInfo);
            jedis.hset(cartkey, skuId, cartJsonStr);

            // 另存一份记录供结算页面使用
            String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
            if ("1".equals(isChecked)) {
                jedis.hset(cartCheckedKey, skuId, cartJsonStr);
            } else {
                jedis.hdel(cartCheckedKey, skuId);
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    @Override
    public List<CartInfo> mergeCartList(List<CartInfo> cartListFromCookie, String userId) {
        /*
            1. 获取DB中的数据，将cookie中的数据与其一一比对
                有则数量相加，更新记录
                无则新添记录
            2. 更新缓存
         */
        // 获取DB中的购物车数据
        List<CartInfo> cartListFromDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartListFromDB != null && cartListFromDB.size() > 0) {
            // 开始合并
            for (CartInfo cartInfoCookie : cartListFromCookie) {
                boolean ifExists = false; // 标志cookie中的sku是否与表中的sku相同
                for (CartInfo cartInfoDB : cartListFromDB) {
                    if (cartInfoCookie.getSkuId().equals(cartInfoDB.getSkuId())) {
                        cartInfoDB.setSkuNum(cartInfoDB.getSkuNum() + cartInfoCookie.getSkuNum());
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB); // 更新数据
                        ifExists = true;
                        break;
                    }
                }

                if (!ifExists) {
                    // 没有相同的sku 向db中新添一条数据
                    cartInfoCookie.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoCookie);
                }
            }
        } else {
            // DB中没有购物车，直接插入
            for (CartInfo cartInfo : cartListFromCookie) {
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }
        }

        // 更新缓存
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
        /*
            更新SKU的勾选状态
                根据cartKey获取缓存数据
                    有， 一一比对修改状态
                    无，······
                根据cartListFromCookie一一比对修改状态
         */
            cartListFromDB = cartInfoMapper.selectCartListWithCurPrice(userId);
            // 根据缓存修改SKU勾选状态
            List<String> hvals = jedis.hvals(cartKey);
            if (hvals != null && hvals.size() > 0) {
                for (String hval : hvals) {
                    CartInfo info = JSON.parseObject(hval, CartInfo.class);
                    for (CartInfo cartInfo : cartListFromDB) {
                        if (info.getSkuId().equals(cartInfo.getSkuId())) {
                            cartInfo.setIsChecked(info.getIsChecked());
                            break;
                        }
                    }
                }
            }
            // 根据本地cookie修改SKU状态
            for (CartInfo info : cartListFromCookie) {
                for (CartInfo cartInfo : cartListFromDB) {
                    if (info.getSkuId().equals(cartInfo.getSkuId())) {
                        if ("1".equals(info.getIsChecked())) {
                            cartInfo.setIsChecked(info.getIsChecked());
                        }
                        break;
                    }
                }
            }

            Map<String, String> map = new HashMap<>(cartListFromDB.size());
            // 另存一份记录供结算页面使用
            String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
            for (CartInfo cartInfo : cartListFromDB) {
                map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
                if ("1".equals(cartInfo.getIsChecked())) {
                    jedis.hset(cartCheckedKey, cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
                }
            }
            jedis.hmset(cartKey, map);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return getCartList(userId);
    }


    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
            根据cartKey先走缓存，若缓存中没有，走DB，并将数据放入缓存
         */
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = null;
        List<CartInfo> cartInfoList = new ArrayList<>();
        try {
            jedis = redisUtil.getJedis();
            Map<String, String> cartMap = jedis.hgetAll(cartKey);
            if (cartMap != null && cartMap.size() > 0) { // 缓存中有数据
                for (String cartInfo : cartMap.values()) {
                    cartInfoList.add(JSON.parseObject(cartInfo, CartInfo.class));
                }
            } else { // 缓存中没有数据 走DB
                cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
                if (cartInfoList != null && cartInfoList.size() > 0) {
                    // 数据库中有数据，更新缓存
                    for (CartInfo cartInfo : cartInfoList) {
                        jedis.hset(cartKey, cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
                    }
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        // 排序 后添加的在上边
        if (cartInfoList != null) {
            cartInfoList.sort((o1, o2) -> Long.compare(Long.parseLong(o2.getId()), Long.parseLong(o1.getId())));
        }


        return cartInfoList;

    }

    @Override
    public void addToCart(String userId, String skuId, Integer skuNum) {

        /*
            1. 根据skuId和userId查看购物车中是否有相同的商品
                有则数量相加
                无则直接添加
            2. 更新缓存中的数据
         */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        CartInfo one = cartInfoMapper.selectOne(cartInfo);

        if (one != null) {
            // 有则数量相加
            one.setSkuNum(one.getSkuNum() + skuNum);
            // 更新实时价格
            one.setSkuPrice(one.getCartPrice());
            // 更新
            cartInfoMapper.updateByPrimaryKeySelective(one);
        } else {
            // 无则直接添加
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuId(skuInfo.getId());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.insertSelective(cartInfo1);
            one = cartInfo1;
        }

        //  更新缓存
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            // key
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            // 更新缓存中的数据
            jedis.hset(cartKey, skuId, JSON.toJSONString(one));
            // 更新缓存中数据的失效时间 可以设置成和用户的过期时间一致，这里我们不做

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

}
