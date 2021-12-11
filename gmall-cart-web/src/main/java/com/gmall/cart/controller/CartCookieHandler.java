package com.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.CartInfo;
import com.gmall.bean.SkuInfo;
import com.gmall.service.ManageService;
import com.gmall.web.util.CookieUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置 cookie 过期时间
    private int COOKIE_CART_MAXAGE = 7 * 24 * 3600; // 7天

    @Reference
    private ManageService manageService;

    /**
     * 未登录，从本地cookie中获取购物车列表
     *
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {

        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);

        return cartInfoList;
    }

    /**
     * 未登录 将购物车数据放入cookie
     *
     * @param request
     * @param response
     * @param skuId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, int skuNum) {

        /*
            1. 先查看cookie中是否有相同的商品
                有则更新
                无则直接添加

         */
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExists = false; // 是否有相同商品的标志
        if (cookieValue != null) {
            // 有，更新
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                if (skuId.equals(cartInfo.getSkuId())) {
                    // 更新数量
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    // 设置实时价格
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExists = true;
                    break;
                }
            }
        }

        if (!ifExists) {
            // 无则直接添加
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuInfo.getId());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoList.add(cartInfo);
        }

        // 存入cookie
        String cartJson = JSON.toJSONString(cartInfoList);

        CookieUtil.setCookie(request, response, cookieCartName, cartJson, COOKIE_CART_MAXAGE, true);

    }

    /**
     * 删除本地购物车cookie
     *
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, cookieCartName);
    }

    /**
     * 更新本地cookie购物车的勾选状态
     *
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {

        List<CartInfo> cartList = getCartList(request);

        for (CartInfo cartInfo : cartList) {
            if (skuId.equals(cartInfo.getSkuId())) {
                cartInfo.setIsChecked(isChecked);
                break;
            }
        }

        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartList), COOKIE_CART_MAXAGE, true);

    }

}
