package com.example.demo.rabbitMQ.consumer;


import com.alibaba.fastjson.JSON;
import com.example.demo.model.viewModel.Person;
import com.example.demo.rabbitMQ.BaseRabbitMqHandler;
import com.example.demo.rabbitMQ.RabbitMQConfig;
import com.example.demo.rabbitMQ.RabbitMqMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
//@RabbitListener(queues = "DirectExchangeQueueSpringBoot")//参数为队列名称
public class DirectExchangeConsumer extends BaseRabbitMqHandler<RabbitMqMessage> {
    @Autowired
    private ObjectMapper objectMapper;
    private static Logger logger = LogManager.getLogger(DirectExchangeConsumer.class);

//    public static final String DIRECT_QUEUE_NAME = "DirectExchangeQueueSpringBoot";


    //发送什么类型就用什么类型接收
    //多个方法绑定同一个队列MQ会轮训发送给各个方法消费
//    @RabbitHandler
//    @RabbitListener(queues = RabbitMQConfig.DIRECT_QUEUE_NAME)//参数为队列名称
    public void receivedMsg(RabbitMqMessage rabbitMqMessage,
                            Channel channel,
                            Message message,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                            @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) throws Exception {
        try {
            //  System.out.println("DirectExchange Queue:" + DIRECT_QUEUE_NAME + " receivedMsg: " + receivedMessage);
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            String exchange = message.getMessageProperties().getReceivedExchange();
            String queueName1 = message.getMessageProperties().getConsumerQueue();
            Person person = null;
            System.out.println("DirectExchange Queue:" + RabbitMQConfig.DIRECT_QUEUE_NAME + " receivedMsg: ");
            super.onMessage(rabbitMqMessage, message, channel, (msg, ch) -> {
                //业务处理
                String msgContent = msg.getContent();
                int m = Integer.parseInt("d");
                logger.info("MQ接收到消息jsonStr : " + msgContent);

            });
            //设置异常进入死信队列
//            Integer m = Integer.parseInt("m");
            //手动Ack listener.simple.acknowledge-mode: manual
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {


            //死信
            // channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);

            //  设置死信队列设置自动Ack. 否则不能进入死信队列。
            //将异常抛出，不能吞了，否则不能重试。和mybatis的事务回滚有点像，否则mybatis不能回滚。
            //  throw new AmqpRejectAndDontRequeueException(e.getMessage()) ;
            throw e;
            // e.printStackTrace();
//            logger.error(e.getMessage());
            //丢弃这条消息
            //channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
        }

    }

    //发送什么类型就用什么类型接收
    //多个方法绑定同一个队列MQ会轮训发送给各个方法消费
    //string 接收
//    @RabbitHandler
//    @RabbitListener(queues = RabbitMQConfig.DIRECT_QUEUE_NAME)//参数为队列名称
    public void receivedMsg(String receivedMessage, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                            @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) throws Exception {
        try {
            //  System.out.println("DirectExchange Queue:" + DIRECT_QUEUE_NAME + " receivedMsg: " + receivedMessage);

            Person person = JSON.parseObject(receivedMessage, Person.class);
            System.out.println("DirectExchange Queue:" + RabbitMQConfig.DIRECT_QUEUE_NAME + " receivedMsg: " + receivedMessage);
//            super.onMessage(deliveryTag, queueName, channel, (msg, ch) -> {
//                        //业务处理
//                        String errorDetail = msg.getContent();
//                        logger.info("MQ tBCustListErrorInfoConsumer接收到消息jsonStr : " + errorDetail);
//
//                    }
//            );
            //设置异常进入死信队列
//            Integer m = Integer.parseInt("m");
            //手动Ack listener.simple.acknowledge-mode: manual
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {


            //死信
            // channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);

            //  设置死信队列设置自动Ack. 否则不能进入死信队列。
            //将异常抛出，不能吞了，否则不能重试。和mybatis的事务回滚有点像，否则mybatis不能回滚。
            //  throw new AmqpRejectAndDontRequeueException(e.getMessage()) ;
            throw e;
            // e.printStackTrace();
//            logger.error(e.getMessage());
            //丢弃这条消息
            //channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
        }

    }

    //发送什么类型就用什么类型接收
    //多个方法绑定同一个队列MQ会轮训发送给各个方法消费
    //多线程消费，prefetch 没起作用，每个线程每次消费一条
    @RabbitHandler
    @RabbitListener(queues = RabbitMQConfig.BATCH_DIRECT_QUEUE_NAME, containerFactory = "multiplyThreadContainerFactory")
    public void consumerByMultiThread(Message message, Channel channel) throws Exception {
        try {
//            Thread.sleep(10000);
            String receivedMessage = new String(message.getBody());
//            RabbitMqMessage rabbitMqMessage = this.objectMapper.readValue(receivedMessage, RabbitMqMessage.class);
//            Person person = objectMapper.readValue(rabbitMqMessage.getContent(), Person.class);

            //logger.info("threadId - " + Thread.currentThread().getId() + " receivedMsg: " + person.getId());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {


            //死信
            // channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);

            //  设置死信队列设置自动Ack. 否则不能进入死信队列。
            //将异常抛出，不能吞了，否则不能重试。和mybatis的事务回滚有点像，否则mybatis不能回滚。
            //  throw new AmqpRejectAndDontRequeueException(e.getMessage()) ;
            throw e;
            // e.printStackTrace();
//            logger.error(e.getMessage());
            //丢弃这条消息
            //channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
        }

    }


    //region  rabbitmq_delayed_message_exchange
    public static final String DELAYED_MESSAGE_EXCHANGE = "DelayedMessageSpringBoot";
    // 路由键支持模糊匹配，符号“#”匹配一个或多个词，符号“*”匹配不多不少一个词
    public static final String DELAYED_MESSAGE_KEY = "DelayedMessageRoutingKeySpringBoot";
    public static final String DELAYED_MESSAGE_QUEUE = "DelayedMessageQueueSpringBoot";

    //endregion
    @RabbitHandler
    @RabbitListener(queues = DELAYED_MESSAGE_QUEUE)
    //使用 @Payload 和 @Headers注解可以消息中的 body 与 headers 信息
    //参数为队列名称  , @Headers Map<String, Object> header
//    public void receivedDelayedMsg(String receivedMessage, Channel channel, Message message, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
    public void receivedDelayedMsg(String receivedMessage, Channel channel, Message message) throws Exception {
        try {
//            receivedMessage =msg
            String msg = new String(message.getBody());
            //  System.out.println("DirectExchange Queue:" + DIRECT_QUEUE_NAME + " receivedMsg: " + receivedMessage);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");
            String dateStr = dateTimeFormatter.format(LocalDateTime.now());
            Person person = JSON.parseObject(receivedMessage, Person.class);
            System.out.println(dateStr + ":" + "DirectExchange Queue:" + DELAYED_MESSAGE_QUEUE + " receivedMsg: " + receivedMessage);
            //设置异常进入死信队列
//            Integer m = Integer.parseInt("m");
            //手动Ack listener.simple.acknowledge-mode: manual
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {


            //死信
            // channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);

            //  设置死信队列设置自动Ack. 否则不能进入死信队列。
            //将异常抛出，不能吞了，否则不能重试。和mybatis的事务回滚有点像，否则mybatis不能回滚。
            //  throw new AmqpRejectAndDontRequeueException(e.getMessage()) ;
            throw e;
            // e.printStackTrace();
//            logger.error(e.getMessage());
            //丢弃这条消息
            //channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
        }


    }
}
