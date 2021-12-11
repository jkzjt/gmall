package com.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.CartInfo;
import com.gmall.bean.OrderDetail;
import com.gmall.bean.OrderInfo;
import com.gmall.bean.UserAddress;
import com.gmall.bean.enums.OrderStatus;
import com.gmall.bean.enums.ProcessStatus;
import com.gmall.service.CartService;
import com.gmall.service.OrderService;
import com.gmall.service.UserService;
import com.gmall.web.annotation.LoginRequire;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.standard.expression.OrExpression;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request) {

        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId, wareSkuMap);
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map<String, Object> map = orderService.initWareOrder(orderInfo);
            mapList.add(map);
        }

        return JSON.toJSONString(mapList);
    }

    @RequestMapping("/submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo) {
        /*
            1. 校验流水号
            2. 校验库存是否足够
            2. 生成订单
         */

        String userId = (String) request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        // 校验流水号
        boolean flag = orderService.validateTradeNo(userId, tradeNo);
        if (!flag) {
            request.setAttribute("errMsg", "该页面已失效，请重新结算!");
            return "tradeFail";
        }
        // 校验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean hasStock = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!hasStock) {
                request.setAttribute("errMsg", orderDetail.getSkuName() + "库存不足!");
                return "tradeFail";
            }
        }

        // 生成订单
        // 初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setUserId(userId);
        orderInfo.sumTotalAmount();
        // 保存
        String orderId = orderService.saveOrder(orderInfo);

        // 删除流水号
        orderService.delTradeNo(userId);
        // 删除购物车 TODO

        // 重定向到支付页面
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }

    @RequestMapping("/trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        // 根据userId获取用户收货地址
        List<UserAddress> addressList = userService.getUserAddressListByUserId(userId);

        request.setAttribute("addressList", addressList);

        // 获取缓存中用户勾选的购物车
        List<CartInfo> checkedCartList = cartService.getCheckedCartList(userId);

        // 订单明细
        List<OrderDetail> orderDetailList = null;
        if (checkedCartList != null && checkedCartList.size() > 0) {
            orderDetailList = new ArrayList<>(checkedCartList.size());
            OrderDetail orderDetail;
            for (CartInfo cartInfo : checkedCartList) {
                orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetailList.add(orderDetail);
            }
        }

        request.setAttribute("orderDetailList", orderDetailList);

        // 订单总金额
        BigDecimal totalAmount = new BigDecimal("0");
        if (orderDetailList != null && orderDetailList.size() > 0) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderDetailList(orderDetailList);
            orderInfo.sumTotalAmount();
            totalAmount = orderInfo.getTotalAmount();
        }

        request.setAttribute("totalAmount", totalAmount);

        // 流水号 防止表单重复提交
        String tradeNo = orderService.genTradeNo(userId);

        request.setAttribute("tradeNo", tradeNo);


        return "trade";
    }

}
