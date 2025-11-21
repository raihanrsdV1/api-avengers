package com.careforall.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class RabbitMQConfig {

    public static final String PLEDGE_EXCHANGE = "pledge.exchange";
    public static final String PLEDGE_CREATED_QUEUE = "pledge.created.queue";

    public static final String DONATION_EXCHANGE = "donation.exchange";
    public static final String DONATION_CAPTURED_ROUTING_KEY = "donation.captured";
    public static final String DONATION_AUTHORIZED_ROUTING_KEY = "donation.authorized";
    public static final String DONATION_FAILED_ROUTING_KEY = "donation.failed";

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

    /**
     * JSON Message Converter for serializing and deserializing messages
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Configure RabbitTemplate with JSON converter for sending messages
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Configure listener container factory with JSON converter for consuming messages
     */
    @Bean
    @DependsOn("rabbitAdmin")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}
