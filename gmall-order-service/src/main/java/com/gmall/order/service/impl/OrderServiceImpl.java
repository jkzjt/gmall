package com.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.OrderDetail;
import com.gmall.bean.OrderInfo;
import com.gmall.bean.enums.ProcessStatus;
import com.gmall.common.util.HttpClientUtil;
import com.gmall.manage.util.ActiveMQUtil;
import com.gmall.manage.util.RedisUtil;
import com.gmall.order.mapper.OrderDetailMapper;
import com.gmall.order.mapper.OrderInfoMapper;
import com.gmall.service.OrderService;
import com.gmall.service.PaymentService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.Queue;
import javax.jms.*;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        // 怎么拆？
        // 1. 根据orderId查询父订单
        OrderInfo parentOrder = getOrderById(orderId);
        // 2. 反序列化wareSkuMap
        /*
            [
                {
                    "wareId":"1",
                    "skuIds":["2","10"]
                },
                {
                    "wareId":"2",
                    "skuIds":["3"]
                }
            ]
         */
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);

        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        // 拆单
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 创建子订单对象
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性对拷
            BeanUtils.copyProperties(parentOrder, subOrderInfo);
            // ID置空
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(parentOrder.getId());
            subOrderInfo.setWareId(wareId);

            // 订单明细
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (String skuId : skuIds) {
                for (OrderDetail orderDetail : parentOrder.getOrderDetailList()) {
                    if (orderDetail.getSkuId().equals(skuId)) {
                        orderDetail.setId(null); // 必须置空，否则插入时报错
                        subOrderDetailList.add(orderDetail);
                        break; // 找到了直接跳出循环
                    }
                }
            }
            subOrderInfo.setOrderDetailList(subOrderDetailList);

            // totalAmount
            subOrderInfo.sumTotalAmount();

            // 保存子订单
            saveOrder(subOrderInfo);

            // 添加
            subOrderInfoList.add(subOrderInfo);
        }

        // 修改父订单的状态
        updateOrderStatus(parentOrder.getId(), ProcessStatus.SPLIT);


        return subOrderInfoList;
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {

        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime", new Date()).andEqualTo("processStatus", ProcessStatus.UNPAID);
        List<OrderInfo> expiredOrderList = orderInfoMapper.selectByExample(example);

        return expiredOrderList;
    }

    @Override
    public void execExpiredOrder(OrderInfo expiredOrder) {

        // 关闭订单
        updateOrderStatus(expiredOrder.getId(), ProcessStatus.CLOSED);
        // 关闭支付
        paymentService.closePayment(expiredOrder.getId());

    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(initWareOrder(orderId));
            producer.send(activeMQTextMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化仓储订单
     *
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        // 根据orderId获取订单信息
        OrderInfo orderInfo = getOrderById(orderId);
        Map<String, Object> map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }


    @Override
    public Map<String, Object> initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();

        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<Map<String, Object>> list = new ArrayList<>(orderDetailList.size());
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            detailMap.put("skuName", orderDetail.getSkuName());
            list.add(detailMap);
        }
        map.put("details", list);

        return map;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }

    @Override
    public OrderInfo getOrderById(String orderId) {
        // 获取订单
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        // 获取订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfo.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        // 将订单明细注入订单
        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(result);
    }

    @Transactional
    @Override
    public String saveOrder(OrderInfo orderInfo) {

        if (orderInfo.getParentOrderId() == null || "".equals(orderInfo.getParentOrderId())) {
            // 初始化参数
            orderInfo.setCreateTime(new Date()); // 订单创建时间
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            orderInfo.setExpireTime(calendar.getTime()); // 订单失效时间
            // 生成第三方支付编号
            String outTradeNo = "GMALL" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
            orderInfo.setOutTradeNo(outTradeNo); // 设置第三方支付编号
        }
        // 插入数据库
        orderInfoMapper.insertSelective(orderInfo);

        /*
            保存订单明细
         */

        String orderId = orderInfo.getId(); // 订单ID
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList(); // 订单明细列表
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insertSelective(orderDetail);
            }
        }


        return orderId;
    }

    @Override
    public boolean validateTradeNo(String userId, String tradeNo) {
        /*
            1. 根据key去缓存中获取流水号
            2. 验证
         */
        // 定义流水号的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeNoRedis = jedis.get(tradeNoKey);
            return tradeNo.equals(tradeNoRedis);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    @Override
    public void delTradeNo(String userId) {
        // 删除流水号
        // 定义流水号的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            jedis.del(tradeNoKey); // 删除缓存中的流水号
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    @Override
    public String genTradeNo(String userId) {

        /*
            1. 生成流水号
            2. 将流水号存入缓存
            3. 返回流水号
         */
        String tradeNo = UUID.randomUUID().toString();

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeNoKey = "user:" + userId + ":tradeCode";
            // 设置成10分钟失效
            jedis.setex(tradeNoKey, 10 * 60, tradeNo);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return tradeNo;
    }
}
