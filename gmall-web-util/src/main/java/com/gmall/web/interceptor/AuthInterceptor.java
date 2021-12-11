package com.gmall.web.interceptor;

import com.alibaba.fastjson.JSON;
import com.gmall.common.util.HttpClientUtil;
import com.gmall.web.annotation.LoginRequire;
import com.gmall.web.consts.WebConst;
import com.gmall.web.util.CookieUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 访问控制器之前执行
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getParameter("newToken");

        if (token != null) {
            // 将token存入cookie
            CookieUtil.setCookie(request, response, "token", token, WebConst.cookieMaxAge, false);
        }

        if (token == null) { // 从cookie中获取token
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        if (token != null) {
            // 解析token， 从中获取用户昵称
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (loginRequire != null) {
            String salt = request.getHeader("x-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_URL + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)) {
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId", userId);
                return true;
            } else {
                if (loginRequire.autoRedirect()) {
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL--------------:" + requestURL);
                    String encodeURI = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println("encodeURI++++++++++++++++++++++:" + encodeURI);
                    response.sendRedirect(WebConst.LOGIN_URL + "?originUrl=" + encodeURI);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 根据token获取用户信息
     *
     * @param token
     * @return
     */
    private Map getUserMapByToken(String token) {

        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);

        String tokenJson = null;
        try {
            tokenJson = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;

    }
}
