package com.example.demo.rabbitMQ;

import cn.hutool.json.JSONUtil;
import com.example.demo.model.entity.demo.MqFailLog;
import com.example.demo.service.demo.IMqFailLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author ruili
 */
public class BaseRabbitMqHandler<T extends RabbitMqMessage> {

    private static final Logger logger = LoggerFactory.getLogger(BaseRabbitMqHandler.class);

    private static final String RABBIT_MQ_MESSAGE_ID_PREFIX = "rabbitMQ:messageId:";
    //
    private static final int TOTAL_RETRY_COUNT = 4;
    private static final int EXPIRE_TIME = 24 * 60 * 60;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    IMqFailLogService mqFailLogService;
    ObjectMapper objectMapper = new ObjectMapper();

    public void onMessage(T t, Message message, Channel channel,
                          RabbitMqHandler<T> mqListener) {


        String mqMsgIdKey = RABBIT_MQ_MESSAGE_ID_PREFIX + t.getMessageId();
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        //添加重复消费redis 校验，不会存在并发同一个message
        Object retryCountObj = valueOperations.get(mqMsgIdKey);
//        String time1 = LocalDateTimeUtil.formatNormal(t.getMessageTime());
//        String time2 = LocalDateTimeUtil.formatNormal(LocalDateTime.now());
//        logger.info("time1 - {} time2 - {}", time1, time2);
        logger.info("开始消费msg - {}", t.getMessageId());
        int retryCount = 0;
        try {

            if (retryCountObj == null) {
                //value 重试次数
                valueOperations.set(mqMsgIdKey, 0);
            } else {
                //没有过期时间,说明没有消费成功
                if (redisTemplate.getExpire(mqMsgIdKey) == -1) {
                    retryCount = (int) retryCountObj;
                    //没有重试
                    if (retryCount == 0) {
                        long deliveryTag = message.getMessageProperties().getDeliveryTag();
                        //补偿 ack--消费了却没有ack 成功。
                        channel.basicAck(deliveryTag, false);
                        logger.info("msgId - {} 已经被消费,msg - {}", t.getMessageId(), objectMapper.writeValueAsString(t));
                        return;
                    }
                } else {
                    logger.info("msgId - {} 已经被消费,msg - {}", t.getMessageId(), objectMapper.writeValueAsString(t));
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }


            }

            mqListener.handle(t, channel);
//             int i = Integer.parseInt("m");


            //消费成功设置过期时间删除key.
            if (redisTemplate.expire(mqMsgIdKey, EXPIRE_TIME, TimeUnit.SECONDS)) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                logger.info("消费成功：{}", t.getMessageId());
            }


        } catch (Exception e) {
            logger.info("消息消费失败：", e);
            logger.info("消息消费失败 - {}", JSONUtil.toJsonStr(t));
            try {
                /**
                 * deliveryTag:该消息的index
                 * multiple：是否批量.true:将一次性拒绝所有小于deliveryTag的消息。
                 * requeue：被拒绝的是否重新入队列
                 */
                //channel.basicNack(deliveryTag, false, true);
                this.retry(t, channel, message, retryCount, e.getMessage());
            } catch (IOException | InterruptedException ex) {
                logger.info("被拒绝的消息重新入队列出错", ex);
            }
        }
    }

    private void retry(T t, Channel channel, Message message, int retryCount, String exceptionMsg) throws IOException, InterruptedException {
        //   String redisCountKey = "retry:" + RabbitMqConstants.TB_CUST_LIST_ERROR_QUEUE + t.getMessageId();
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String mqMsgIdKey = RABBIT_MQ_MESSAGE_ID_PREFIX + t.getMessageId();
        boolean requeue = ++retryCount <= TOTAL_RETRY_COUNT;
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (requeue) {
            channel.basicNack(deliveryTag, false, false);
            valueOperations.set(mqMsgIdKey, retryCount);
            logger.info(" {} 开始第{}次回归到队列：", deliveryTag, retryCount);
        } else {
            //ack 掉消息，把该消息插入数据库，批处理
            if (redisTemplate.expire(mqMsgIdKey, EXPIRE_TIME, TimeUnit.SECONDS)) {
                channel.basicAck(deliveryTag, false);
            }

            HashMap<String, String> map = objectMapper.readValue(t.getContent(), new TypeReference<HashMap<String, String>>() {
            });
            String id = "";
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("id")) {
                    id = entry.getValue();
                    break;
                }
            }

            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            String exchange = message.getMessageProperties().getReceivedExchange();
            String queueName = message.getMessageProperties().getConsumerQueue();
            //错误日志入库
            MqFailLog mqFailLog = new MqFailLog();
            mqFailLog.setMsgContentId(id);
            mqFailLog.setExchange(exchange);
            mqFailLog.setQueueName(queueName);
            mqFailLog.setRoutingKey(routingKey);
            mqFailLog.setMsgId(t.getMessageId());
            mqFailLog.setMessage(JSONUtil.toJsonStr(t));
            mqFailLog.setCause(exceptionMsg);
            mqFailLogService.save(mqFailLog);
        }

    }
}
