package com.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.*;
import com.gmall.manage.constant.ManageConst;
import com.gmall.manage.mapper.*;
import com.gmall.manage.util.RedisUtil;
import com.gmall.manage.util.RedissonUtil;
import com.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedissonUtil redissonUtil;

    @Override
    public List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList) {
        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        // System.out.println(attrValueIds);
        return baseAttrInfoMapper.selectByAttrValueIds(attrValueIds);
    }

    @Override
    public List<SkuAttrValue> getSkuAttrValueList(String skuId) {
        Example example = new Example(SkuAttrValue.class);
        example.selectProperties("valueId").createCriteria().andEqualTo("skuId", skuId);
        return skuAttrValueMapper.selectByExample(example);
    }

    @Override
    public List<SkuInfo> getSkuInfoList(String spuId) {
        return skuInfoMapper.selectBySpuId(spuId);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectBySkuIdAndSpuId(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        /*
         * 进来先走redis，命中直接返回，否则走DB，将从DB中查出来的数据放入redis，再返回
         */

        String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis.exists(skuKey)) { // redis中存在skuKey
                return JSON.parseObject(jedis.get(skuKey), SkuInfo.class);
            } else { // redis中不存在skuKey
                /*
                    // 不加锁
                    SkuInfo skuInfo = skuInfoMapper.selectByPK(skuId);
                    // 放入redis
                    jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    return skuInfo;
                */

                String skuLock = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;

                // redis 锁
                // return getSkuInfoRedisLock(jedis, skuId, skuKey, skuLock);

                // redisson 锁 注意锁的细粒度问题
                return getSkuInfoRedissonLock(jedis, skuId, skuKey, skuLock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return skuInfoMapper.selectByPK(skuId);
    }

    private SkuInfo getSkuInfoRedissonLock(Jedis jedis, String skuId, String skuKey, String skuLock) throws InterruptedException {
        // 分布式锁解决缓存击穿问题 redisson lock
        RedissonClient redissonClient = redissonUtil.getRedissonClient();
        RLock lock = redissonClient.getLock(skuLock);

        SkuInfo skuInfo = null;

        if (lock.tryLock()) { // 上锁成功

            try {
                skuInfo = skuInfoMapper.selectByPK(skuId);

                // 放入redis
                jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

            } finally {
                // 释放锁
                lock.unlock();
            }

        } else {
            // 自旋
            Thread.sleep(ManageConst.WAIT_TIME);

            jedis.close();

            getSkuInfo(skuId);
        }

        return skuInfo;
    }

    private SkuInfo getSkuInfoRedisLock(Jedis jedis, String skuId, String skuKey, String skuLock) throws InterruptedException {
        // 分布式锁解决缓存击穿问题 redis set k v [EX|PX] NX 或 setnx k v
        String ok = jedis.set(skuLock, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
        SkuInfo skuInfo = null;

        if ("OK".equals(ok)) { // 加锁成功
            skuInfo = skuInfoMapper.selectByPK(skuId);

            // 放入redis
            jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

            // 释放锁
            jedis.del(skuLock);
        } else { // 加锁不成功
            // 自旋
            Thread.sleep(ManageConst.WAIT_TIME);

            jedis.close();

            getSkuInfo(skuId);
        }
        return skuInfo;
    }

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        if (skuInfo != null) {
            // 插入SkuInfo
            skuInfoMapper.insertSelective(skuInfo);

            List<SkuAttrValue> attrValueList = skuInfo.getSkuAttrValueList();
            if (attrValueList != null && attrValueList.size() > 0) {
                // 插入SkuAttrValue
                for (SkuAttrValue skuAttrValue : attrValueList) {
                    skuAttrValue.setSkuId(skuInfo.getId());
                    skuAttrValueMapper.insertSelective(skuAttrValue);
                }
            }

            List<SkuSaleAttrValue> saleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            if (saleAttrValueList != null && saleAttrValueList.size() > 0) {
                // 插入SkuSaleAttrValue
                for (SkuSaleAttrValue skuSaleAttrValue : saleAttrValueList) {
                    skuSaleAttrValue.setSkuId(skuInfo.getId());
                    skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
                }
            }

            List<SkuImage> skuImageList = skuInfo.getSkuImageList();
            if (skuImageList != null && skuImageList.size() > 0) {
                // 插入SkuImage
                for (SkuImage skuImage : skuImageList) {
                    skuImage.setSkuId(skuInfo.getId());
                    skuImageMapper.insertSelective(skuImage);
                }
            }

        }
    }

    @Override
    public List<SpuImage> spuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.spuSaleAttrList(spuId);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        if (spuInfo != null) {
            // 保存spuInfo
            spuInfoMapper.insertSelective(spuInfo);

            // 保存spuImage
            List<SpuImage> spuImageList = spuInfo.getSpuImageList();
            if (spuImageList != null && spuImageList.size() > 0) {
                for (SpuImage spuImage : spuImageList) {
                    spuImage.setSpuId(spuInfo.getId());
                    spuImageMapper.insertSelective(spuImage);
                }
            }

            // 保存销售属性
            List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
            if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
                for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                    spuSaleAttr.setSpuId(spuInfo.getId());
                    spuSaleAttrMapper.insertSelective(spuSaleAttr);

                    // 保存销售属性值
                    List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                    if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                        for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                            spuSaleAttrValue.setSpuId(spuInfo.getId());
                            spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                        }
                    }
                }
            }

        }
    }

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public List<SpuInfo> spuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public BaseAttrInfo getAttrValueList(String attrId) {
        // 根据attrId查询BaseAttrInfo
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 根据attrId查询属性值列表
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(baseAttrValue);

        baseAttrInfo.setAttrValueList(attrValueList);

        return baseAttrInfo;
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 判断有无主键
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            // 有主键，更新
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {
            // 无主键，插入
            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        // 插入属性值之前将原有的属性值清空
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);
        // 插入属性值
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                // 避免空字符串
                attrValue.setId(null);
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }

    }

    @Override
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        return baseAttrInfoMapper.attrInfoList(catalog3Id);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

}
