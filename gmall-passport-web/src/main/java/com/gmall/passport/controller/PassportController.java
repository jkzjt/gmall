package com.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.UserInfo;
import com.gmall.passport.util.JwtUtil;
import com.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${token.key}")
    private String tokenKey;

    @Reference
    private UserService userService;

    @RequestMapping("/verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        // 解析token，拿到用户ID进行校验
        Map<String, Object> map = JwtUtil.decode(token, tokenKey, salt);
        if (map != null && map.size() > 0) {

            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }

    @RequestMapping("/login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        if (userInfo != null) {
            UserInfo info = userService.login(userInfo);
            if (info != null) {
                // 盐
                String salt = request.getHeader("X-forwarded-for");
                Map<String, Object> map = new HashMap<>();
                map.put("userId", info.getId());
                map.put("nickName", info.getNickName());
                // 生成token
                String token = JwtUtil.encode(tokenKey, map, salt);
                return token;
            }
        }
        return "fail";
    }

    @RequestMapping("/index")
    public String index(HttpServletRequest request) {

        String originUrl = request.getParameter("originUrl");

        request.setAttribute("originUrl", originUrl);

        return "index";
    }

}
