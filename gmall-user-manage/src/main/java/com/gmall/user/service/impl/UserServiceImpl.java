package com.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.UserAddress;
import com.gmall.bean.UserInfo;
import com.gmall.manage.util.RedisUtil;
import com.gmall.service.UserService;
import com.gmall.user.mapper.UserAddressMapper;
import com.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    public String userKey_prefix = "user:";
    public String userKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60 * 24; // 1d


    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public UserInfo verify(String userId) {
        // 根据用户ID查看缓存
        String userKey = userKey_prefix + userId + userKey_suffix;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String userInfo = jedis.get(userKey);
            if (userInfo != null) {
                // 更新有效期
                jedis.expire(userKey, userKey_timeOut);
                return JSON.parseObject(userInfo, UserInfo.class);
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public UserInfo login(UserInfo userInfo) {

        // 将前台传递的明文密码加密成暗文并设置
        userInfo.setPasswd(DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes()));

        UserInfo info = userInfoMapper.selectOne(userInfo);

        if (info != null) {
            Jedis jedis = null;
            try {
                // 校验通过并将用户信息存入redis
                jedis = redisUtil.getJedis();
                jedis.setex(userKey_prefix + info.getId() + userKey_suffix, userKey_timeOut, JSON.toJSONString(info));
                return info;
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }

        return null;
    }

    @Override
    public List<UserAddress> getUserAddressListByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId("1");
        return userAddressMapper.select(userAddress);
    }

    @Override
    public List<UserInfo> listAllUserInfo() {
        return userInfoMapper.selectAll();
    }
}
