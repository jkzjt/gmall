package com.gmall.manage.conf;

import com.gmall.manage.util.RedisUtil;
import com.gmall.manage.util.RedissonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class RedisConf {

    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean
    public RedisUtil getRedisUtil() {
        if ("disabled".equals(host)) {
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host, port, database);
        return redisUtil;
    }

    @Bean
    public RedissonUtil getRedissonUtil() {
        if ("disabled".equals(host)) {
            return null;
        }
        RedissonUtil redissonUtil = new RedissonUtil();
        redissonUtil.initConfig(host, port, database);
        return redissonUtil;
    }

}
