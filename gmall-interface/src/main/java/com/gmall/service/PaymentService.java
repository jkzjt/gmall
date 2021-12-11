package com.gmall.service;

import com.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    /**
     * 保存支付信息
     *
     * @param paymentInfo
     */
    void savePaymentInfoSelective(PaymentInfo paymentInfo);

    /**
     * 退款
     *
     * @param orderId
     * @return
     */
    String refund(String orderId);

    /**
     * 根据条件获取支付信息
     *
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 更新支付信息
     *
     * @param paymentInfoDB
     */
    void modifyPaymentInfo(PaymentInfo paymentInfoDB);

    /**
     * 微信支付 Native
     *
     * @param orderId
     * @param totalAmount
     * @return
     */
    Map<String, Object> createNative(String orderId, String totalAmount);

    /**
     * 发送支付结果消息
     *
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    /**
     * 查询是否支付成功
     *
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 延时队列查询结果
     *
     * @param outTradeNo 第三方交易编号
     * @param delaySec   延迟时间
     * @param checkCount 轮询次数
     */
    void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);

    /**
     * 关闭支付信息
     *
     * @param orderId
     */
    void closePayment(String orderId);
}
