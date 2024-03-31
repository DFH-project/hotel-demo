package cn.itcast.hotel.config;

import cn.itcast.hotel.constants.MQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    @Bean
    public TopicExchange topicExchange(){
        // durable 设置持久化，存到硬盘里
        return new TopicExchange(MQConstants.HOTEL_EXCHANGE,true,false);
    }

    @Bean
    public Queue insertQueue(){
        return new Queue(MQConstants.HOTEL_INSERT_QUEUE,true);
    }

    @Bean
    public Queue delQueue(){
        return new Queue(MQConstants.HOTEL_DELETE_QUEUE,true);
    }

    @Bean
    public Binding insertQueueBinding(){
        return BindingBuilder.bind(insertQueue()).to(topicExchange()).with(MQConstants.HOTEL_INSERT_KEY);
    }

    @Bean
    public Binding delQueueBinding(){
        return BindingBuilder.bind(delQueue()).to(topicExchange()).with(MQConstants.HOTEL_DELETE_KEY);
    }

}
