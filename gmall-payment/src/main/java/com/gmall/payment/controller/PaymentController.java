package com.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.gmall.bean.OrderInfo;
import com.gmall.bean.PaymentInfo;
import com.gmall.bean.enums.PaymentStatus;
import com.gmall.payment.conf.AlipayConfig;
import com.gmall.service.OrderService;
import com.gmall.service.PaymentService;
import com.gmall.web.annotation.LoginRequire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient; //获得初始化的AlipayClient

    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request) {
        // 获取orderId
        String orderId = request.getParameter("orderId");

        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        if (flag) {
            return "success";
        }
        return "failure";
    }

    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, String result) {
        paymentService.sendPaymentResult(paymentInfo, result);
        return "send payment result";
    }

    // TODO 微信支付
    @RequestMapping("wx/submit")
    @LoginRequire
    @ResponseBody
    public Map<String, Object> wxPay(HttpServletRequest request) {
        // 获取商户订单ID
//        http://payment.gmall.com/index?orderId=101
//        String referer = request.getHeader("Referer");
//        System.out.println(referer);

        String orderId = request.getParameter("orderId");
        String totalAmount = "1";
        Map<String, Object> map = paymentService.createNative(orderId, totalAmount);
        return map;
    }


    // 支付宝异步通知
    @RequestMapping("/alipay/callback/notify")
    @LoginRequire
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramMap) throws AlipayApiException {
        /*
            1、	验证回调信息的真伪
            2、	验证用户付款的成功与否
            3、	把新的支付状态写入支付信息表{paymentInfo}中
            4、	通知电商
            5、	给支付宝返回回执
         */
        // 验证回调信息的真伪 验签
        boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.CHARSET, AlipayConfig.SIGN_TYPE);
        if (!flag) {
            return "failure";
        }
        /* 验签成功 */
        // 验证用户付款的成功与否
        String trade_status = paramMap.get("trade_status");
        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) { // 支付成功
            // 获取商户订单号
            String out_trade_no = paramMap.get("out_trade_no");
            // 验证DB中的支付信息
            PaymentInfo paymentInfoQuery = new PaymentInfo();
            paymentInfoQuery.setOutTradeNo(out_trade_no);
            PaymentInfo paymentInfoDB = paymentService.getPaymentInfo(paymentInfoQuery);
            if (paymentInfoDB != null) {
                PaymentStatus oldPaymentStatus = paymentInfoDB.getPaymentStatus();
                if (oldPaymentStatus.equals(PaymentStatus.PAID) || oldPaymentStatus.equals(PaymentStatus.ClOSED)) {
                    // 验证DB中的支付信息失败
                    return "failure";
                }
                // 验证DB中的支付信息成功
                // 修改支付支付状态
                paymentInfoDB.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoDB.setAlipayTradeNo(paramMap.get("trade_no")); // 支付宝交易凭证号
                paymentInfoDB.setCallbackTime(new Date());
                paymentInfoDB.setCallbackContent(paramMap.toString());
                // 调用服务层
                paymentService.modifyPaymentInfo(paymentInfoDB);
                // 发送消息通知订单模块修改订单状态
                // sendPaymentResult(paymentInfoDB, "success");
                return "success";
            }
        }


        return "failure";
    }

    // 退款
    @RequestMapping("refund")
    @LoginRequire
    @ResponseBody
    public String refund(String orderId) {
        return paymentService.refund(orderId);
    }

    // 同步回调
    @RequestMapping("alipay/callback/return")
    @LoginRequire
    public String callbackReturn() {
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /*
        1、通过orderId取得订单信息
        2、组合对应的支付信息保存到数据库
        3、组合需要传给支付宝的参数
        4、根据返回的表单生成html，传给浏览器
     */
    @RequestMapping("alipay/submit")
    @LoginRequire
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        /*
            获取订单信息 未拆单
        */
        // 获取订单ID
        String orderId = request.getParameter("orderId");
        // 获取订单信息
        OrderInfo orderInfo = orderService.getOrderById(orderId);

        /*
            保存支付信息
         */
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentService.savePaymentInfoSelective(paymentInfo);

        /*
            调用支付宝接口
            alipay.trade.page.pay（统一收单下单并支付页面接口）
         */

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest(); //创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url); // 同步回调URL
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //异步通知URL 在公共参数中设置回跳和通知地址
        // 业务请求参数集合
        Map<String, Object> bizContentMap = new HashMap<>();
        //填充业务参数
        bizContentMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContentMap.put("subject", paymentInfo.getSubject());
        bizContentMap.put("total_amount", paymentInfo.getTotalAmount());
        bizContentMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        // 业务请求参数JSON
        String bizContentJson = JSON.toJSONString(bizContentMap);
        System.out.println(bizContentJson);
        // 设置业务参数
        alipayRequest.setBizContent(bizContentJson);

        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
//        response.getWriter().write(form); //直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();

        // 生成二维码后发送消息轮询结果
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(), 15, 3);

        return form;
    }

    @RequestMapping("/index")
    @LoginRequire
    public String index(HttpServletRequest request) {

        // 支付页面需要的数据，订单ID，订单金额
        // 获取订单ID
        String orderId = request.getParameter("orderId");
        // 获取订单金额
        OrderInfo orderInfo = orderService.getOrderById(orderId);
        BigDecimal totalAmount = orderInfo.getTotalAmount();

        request.setAttribute("orderId", orderId);
        request.setAttribute("totalAmount", totalAmount);

        return "index";
    }

}
