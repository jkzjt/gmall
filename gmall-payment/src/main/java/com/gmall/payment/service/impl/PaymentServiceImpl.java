package com.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.github.wxpay.sdk.WXPayUtil;
import com.gmall.bean.PaymentInfo;
import com.gmall.bean.enums.PaymentStatus;
import com.gmall.common.util.HttpClient;
import com.gmall.manage.util.ActiveMQUtil;
import com.gmall.payment.mapper.PaymentInfoMapper;
import com.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    // 服务号Id
    @Value("${appid}")
    private String appid;

    // 商户号Id
    @Value("${partner}")
    private String partner;

    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public void closePayment(String orderId) {

        // 查询
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        PaymentInfo paymentInfo = getPaymentInfo(paymentInfoQuery);
        if(paymentInfo != null){
            // 修改 PaymentStatus.ClOSED
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
            modifyPaymentInfo(paymentInfo);
        }

    }

    // PAYMENT_RESULT_CHECK_QUEUE 生产者
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo", outTradeNo);
            activeMQMapMessage.setInt("delaySec", delaySec);
            activeMQMapMessage.setInt("checkCount", checkCount);
            // 设置延迟时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec * 1000);
            producer.send(activeMQMapMessage);
            // 提交
            session.commit();
            activeMQUtil.release(producer, session, connection);
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {

        // 需要第三方交易编号 从数据库取
        PaymentInfo paymentInfo = getPaymentInfo(paymentInfoQuery);

        // 检测支付状态
        PaymentStatus paymentStatus = paymentInfo.getPaymentStatus();
        if (paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.ClOSED) {
            return true;
        }

        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
//        JSONObject bizContent = new JSONObject();
//        bizContent.put("out_trade_no", "20150320010101001");
//bizContent.put("trade_no", "2014112611001004680073956707");
        Map<String, Object> bizContentMap = new HashMap<>();
        bizContentMap.put("out_trade_no", paymentInfo.getOutTradeNo());

        request.setBizContent(JSON.toJSONString(bizContentMap));
        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response == null) {
            return false;
        }

        String tradeStatus = response.getTradeStatus();

        if (response.isSuccess() && "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {

            // 交易成功或交易结束
            // 按理来说应该在异步回调时修改支付信息

            paymentInfo.setPaymentStatus(PaymentStatus.PAID);
            paymentInfo.setAlipayTradeNo(response.getTradeNo());
            paymentInfo.setCallbackTime(new Date());
            modifyPaymentInfo(paymentInfo);


            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(payment_result_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId", paymentInfo.getOrderId());
            activeMQMapMessage.setString("result", result);
            producer.send(activeMQMapMessage);
            session.commit();

//            producer.close();
//            session.close();
//            connection.close();
            activeMQUtil.release(producer, session, connection);
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Map<String, Object> createNative(String orderId, String totalAmount) {

        // 创建参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", appid);
        paramMap.put("mch_id", partner);
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        paramMap.put("body", "测试微信支付");
        paramMap.put("out_trade_no", orderId);
        paramMap.put("total_fee", totalAmount);
        paramMap.put("spbill_create_ip", "127.0.0.1");
        paramMap.put("notify_url", "http://order.gmall.com/list");
        paramMap.put("trade_type", "NATIVE");

        // 生成xml
        try {
            String xml = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            System.out.println("xml************" + xml);
            // 发送请求
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xml);
            httpClient.post();
            // 获取结果
            String result = httpClient.getContent();
            System.out.println("result*************" + result);
            // xml转Map
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, Object> map = new HashMap<>();
            map.put("code_url", resultMap.get("code_url"));
            map.put("total_fee", totalAmount);
            map.put("out_trade_no", orderId);

            return map;
        } catch (Exception e) {

            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public void modifyPaymentInfo(PaymentInfo paymentInfoDB) {
        paymentInfoMapper.updateByPrimaryKeySelective(paymentInfoDB);
    }

    @Override
    public String refund(String orderId) {
        // 根据订单ID获取支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo info = getPaymentInfo(paymentInfo);

        // 调用支付宝接口退款
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
//        JSONObject bizContent = new JSONObject();
//        bizContent.put("trade_no", "2021081722001419121412730660");
//        bizContent.put("refund_amount", 0.01);
//        bizContent.put("out_request_no", "HZ01RF001");
        // 请求参数
        Map<String, Object> bizContentMap = new HashMap<>();
        bizContentMap.put("out_trade_no", info.getOutTradeNo());
        bizContentMap.put("refund_amount", info.getTotalAmount());

        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(JSON.toJSONString(bizContentMap));
        AlipayTradeRefundResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response == null) {
            return "退款失败";
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return "退款成功";
        } else {
            System.out.println("调用失败");
            return "退款失败";
        }
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public void savePaymentInfoSelective(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

}
