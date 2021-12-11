package com.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.PaymentInfo;
import com.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {

        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        // 检查支付状态
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        boolean flag = paymentService.checkPayment(paymentInfo);
        System.out.println("检查结果********" + flag);
        if (!flag && checkCount > 0) { // 尚未支付
            System.out.println("检查的次数*******" + checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo, delaySec, checkCount - 1);
        }
        // 已经支付了

    }

}
