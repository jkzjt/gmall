package com.gmall.order.mq;


import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.enums.ProcessStatus;
import com.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @JmsListener(destination = "SKU_DEDUCT_QUEUE", containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {

        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");

        if ("DEDUCTED".equals(status)) {
            orderService.updateOrderStatus(orderId, ProcessStatus.WAITING_DELEVER);
        } else {
            orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
        }

    }

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println(orderId + " : " + result);
        if ("success".equals(result)) {
            // 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 通知锁库存
            orderService.sendOrderStatus(orderId);
            // 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        } else {
            // 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.UNPAID);
        }

    }

}
