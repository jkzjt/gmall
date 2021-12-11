package com.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        /*
            1.  创建连接工厂
            2.  创建连接
            3.  打开连接
            4.  创建session
            5.  创建队列
            6.  创建消息消费者
            7.  消费消息
         */
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.215.141:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("gmall-true");
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(message -> {
            if (message instanceof TextMessage){
                try {
                    String text = ((TextMessage) message).getText();
                    System.out.println(text);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}
