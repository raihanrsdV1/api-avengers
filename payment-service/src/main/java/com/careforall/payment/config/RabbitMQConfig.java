package com.careforall.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PLEDGE_EXCHANGE = "pledge.exchange";
    public static final String PLEDGE_CREATED_QUEUE = "pledge.created.queue";

    public static final String DONATION_EXCHANGE = "donation.exchange";

    @Bean
    public Queue pledgeCreatedQueue() {
        return new Queue(PLEDGE_CREATED_QUEUE, true);
    }

    @Bean
    public TopicExchange pledgeExchange() {
        return new TopicExchange(PLEDGE_EXCHANGE);
    }

    @Bean
    public Binding pledgeCreatedBinding(Queue pledgeCreatedQueue, TopicExchange pledgeExchange) {
        return BindingBuilder.bind(pledgeCreatedQueue).to(pledgeExchange).with("pledge.created");
    }

    @Bean
    public TopicExchange donationExchange() {
        return new TopicExchange(DONATION_EXCHANGE);
    }
}
