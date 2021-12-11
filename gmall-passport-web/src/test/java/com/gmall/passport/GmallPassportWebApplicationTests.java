package com.gmall.passport;


import com.gmall.passport.util.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {

    }

    @Test
    public void testJWT() {
        String key = "gmall";
        String ip = "192.168.215.1";

        Map<String, Object> map = new HashMap<>();
        map.put("userId", "9494");
        map.put("nickname", "张三");

        String token = JwtUtil.encode(key, map, ip);
        System.out.println(token);
        Map<String, Object> userMap = JwtUtil.decode(token, key, ip);
        System.out.println(userMap);
    }

}
