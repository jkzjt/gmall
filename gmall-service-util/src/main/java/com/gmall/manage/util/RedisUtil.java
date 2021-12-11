package com.gmall.manage.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    private JedisPool jedisPool;

    public void initJedisPool(String host, int port, int database) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(200);
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);
        jedisPoolConfig.setMinIdle(10);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPool = new JedisPool(jedisPoolConfig, host, port, 20 * 1000, null, database);
    }

    public Jedis getJedis() {
        return jedisPool.getResource();
    }
}
