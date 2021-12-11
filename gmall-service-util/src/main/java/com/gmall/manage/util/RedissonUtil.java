package com.gmall.manage.util;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonUtil {

    private Config config;

    public void initConfig(String host, int port, int database) {
        config = new Config();
        config
                .useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionPoolSize(200)
                .setConnectionMinimumIdleSize(10);
    }

    public RedissonClient getRedissonClient() {
        return Redisson.create(config);
    }

}
