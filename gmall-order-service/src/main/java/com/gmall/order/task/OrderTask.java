package com.gmall.order.task;


import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.OrderInfo;
import com.gmall.service.OrderService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;

    // 定时处理过期的订单
//    @Async
//    @Scheduled(cron = "0/10 * * * * ?")
    public void checkOrder()  {
        System.out.println(Thread.currentThread().getName() + " 处理过期订单");

        long start = System.currentTimeMillis();

        // 获取过期的订单
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();

        for (OrderInfo expiredOrder : expiredOrderList) {
            // 关闭过期的订单
            orderService.execExpiredOrder(expiredOrder);
        }

        long end = System.currentTimeMillis();
        System.out.println(expiredOrderList.size() + "个过期订单， 耗时" + (end -start) + " ms");

       // Thread.sleep(5 * 1000);

    }

    // 每分钟的第5秒执行一次
    // @Scheduled(cron = "5 * * * * ?")
    public void test1() {
        System.out.println("-------------test1----------------");
    }

    // 每隔5秒执行一次
    // @Scheduled(cron = "0/5 * * * * ?")
    public void test2() {
        System.out.println("-------------test2----------------");
    }
}
