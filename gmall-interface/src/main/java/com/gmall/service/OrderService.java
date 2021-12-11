package com.gmall.service;

import com.gmall.bean.OrderInfo;
import com.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    /**
     * 生成流水号
     *
     * @param userId
     * @return
     */
    String genTradeNo(String userId);

    /**
     * 验证流水号
     *
     * @param userId
     * @param tradeNo
     * @return
     */
    boolean validateTradeNo(String userId, String tradeNo);

    /**
     * 删除流水号
     *
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 保存订单
     *
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 校验商品库存是否满足
     *
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据ID获取订单
     *
     * @param orderId
     * @return
     */
    OrderInfo getOrderById(String orderId);

    /**
     * 修改订单状态
     *
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 发送消息通知仓储系统锁定库存
     *
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询过期的订单
     *
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期的订单
     *
     * @param expiredOrder
     */
    void execExpiredOrder(OrderInfo expiredOrder);

    /**
     * 拆单
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);

    /**
     * 将orderinfo转换为map
     *
     * @param orderInfo
     * @return
     */
    Map<String, Object> initWareOrder(OrderInfo orderInfo);
}
