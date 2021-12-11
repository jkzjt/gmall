package com.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    public static void main(String[] args) throws JMSException {
        /*
            1.  创建连接工厂
            2.  创建连接
            3.  打开连接
            4.  创建session
            5.  创建队列
            6.  创建消息提供者
            7.  创建消息对象
            8.  发送消息
            9.  关闭
         */
        // 1.  创建连接工厂 tcp协议
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.215.141:61616");
        // 2.  创建连接
        Connection connection = activeMQConnectionFactory.createConnection();
        // 3.  打开连接
        connection.start();
        // 4.  创建session
        // transacted 是否开始事务
        // acknowledgeMode 事务的相关配置
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED); // 必须手动提交
        // 5.  创建队列
        Queue queue = session.createQueue("gmall-true");
        // 6.  创建消息提供者
        MessageProducer producer = session.createProducer(queue);
        // 7.  创建消息对象 文本
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Hi, activeMQ");
        // 8.  发送消息i
        producer.send(activeMQTextMessage);
        // 提交
        session.commit();
        // 9.  关闭
        producer.close();
        session.close();
        connection.close();
    }

}
