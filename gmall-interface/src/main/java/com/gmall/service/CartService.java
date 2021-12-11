package com.gmall.service;

import com.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 登录添加购物车
     *
     * @param userId
     * @param skuId
     * @param skuNum
     */
    void addToCart(String userId, String skuId, Integer skuNum);

    /**
     * 登录 本地cookie无购物车数据 从缓存或DB中查询
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 登录 本地cookie有购物车数据 将其与DB中的购物车数据合并，更新缓存并返回
     *
     * @param cartListFromCookie
     * @param userId
     * @return
     */
    List<CartInfo> mergeCartList(List<CartInfo> cartListFromCookie, String userId);

    /**
     * 更新缓存中商品的勾选状态
     *
     * @param userId
     * @param skuId
     * @param isChecked
     */
    void checkCart(String userId, String skuId, String isChecked);

    /**
     * 获取缓存中勾选的购物车
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCheckedCartList(String userId);
}
