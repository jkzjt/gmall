package com.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.CartInfo;
import com.gmall.bean.SkuInfo;
import com.gmall.service.CartService;
import com.gmall.service.ManageService;
import com.gmall.web.annotation.LoginRequire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("/toTrade")
    @LoginRequire // 去结算页 用户必须登录
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList = cartCookieHandler.getCartList(request);
        if (cartList != null && cartList.size() > 0) {
            // 合并购物车 需要注意SKU的勾选状态
            cartService.mergeCartList(cartList, userId);
            // 删除本地购物车cookie
            cartCookieHandler.deleteCartCookie(request, response);
        }

        return "redirect://order.gmall.com/trade";
    }

    @RequestMapping("/checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {

        /*
            根据前台购物车勾选状态更新redis中的商品状态
         */
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");

        if (userId != null) { // 登录
            cartService.checkCart(userId, skuId, isChecked);
        } else { // 未登录
            cartCookieHandler.checkCart(request, response, skuId, isChecked);
        }

    }

    //@LoginRequire
    @LoginRequire(autoRedirect = false)
    @RequestMapping("/cartList")
    public String cartList(HttpServletRequest request, HttpServletResponse response) {

        /*
        根据userId判断用户是否登录
            登录状态下读取本地cookie，cookie不为空，与DB中的购物车列表一一比较，有相同的商品则数量相加，无则直接添加，最后更新缓存
                ；cookie为空，走缓存或DB后返回结果

            未登录状态下从本地cookie中读取数据
        */
        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartInfoList = null;
        if (userId != null) {
            // 登录
            // 读取本地cookie
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            if (cartListFromCookie != null && cartListFromCookie.size() > 0) {
                // 合并购物车
                cartInfoList = cartService.mergeCartList(cartListFromCookie, userId);
                // 删除本地cookie
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                cartInfoList = cartService.getCartList(userId);
            }
        } else {
            // 未登录
            cartInfoList = cartCookieHandler.getCartList(request);
        }

        request.setAttribute("cartInfoList", cartInfoList);

        return "cartList";

    }


    @LoginRequire(autoRedirect = false)
    @RequestMapping("/addToCart")
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {
        // 获取skuId、skuNum、userId 根据userId是否为空判断用户登陆状态
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            // 用户已登录
            cartService.addToCart(userId, skuId, Integer.parseInt(skuNum));
        } else {
            // 用户未登录
            cartCookieHandler.addToCart(request, response, skuId, Integer.parseInt(skuNum));
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";
    }

}
