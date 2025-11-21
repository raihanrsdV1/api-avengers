package com.careforall.pledge.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Pledge Service
 */
@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String PLEDGE_EXCHANGE = "pledge.exchange";

    // Queues
    public static final String PAYMENT_CAPTURED_QUEUE = "payment.captured.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";

    // Routing Keys
    public static final String PAYMENT_CAPTURED_ROUTING_KEY = "payment.captured";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PLEDGE_CREATED_ROUTING_KEY = "pledge.created";

    @Bean
    public TopicExchange pledgeExchange() {
        return new TopicExchange(PLEDGE_EXCHANGE);
    }

    @Bean
    public Queue paymentCapturedQueue() {
        return QueueBuilder.durable(PAYMENT_CAPTURED_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Binding paymentCapturedBinding() {
        return BindingBuilder
                .bind(paymentCapturedQueue())
                .to(pledgeExchange())
                .with(PAYMENT_CAPTURED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(pledgeExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.Jackson2JsonMessageConverter messageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.core.RabbitTemplate template = new org.springframework.amqp.rabbit.core.RabbitTemplate(
                connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
