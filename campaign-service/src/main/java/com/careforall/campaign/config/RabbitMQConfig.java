package com.careforall.campaign.config;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.DependsOn;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Campaign Service
 * Configures the consumer side to listen to donation events
 */
@Configuration
public class RabbitMQConfig {

    // These must match the payment service configuration
    public static final String DONATION_EXCHANGE = "donation.exchange";
    public static final String DONATION_CAPTURED_QUEUE = "donation.captured.queue";
    public static final String DONATION_AUTHORIZED_QUEUE = "donation.authorized.queue";
    public static final String DONATION_FAILED_QUEUE = "donation.failed.queue";

    @Bean
    public Queue donationCapturedQueue() {
        return new Queue(DONATION_CAPTURED_QUEUE, true);
    }

    @Bean
    public Queue donationAuthorizedQueue() {
        return new Queue(DONATION_AUTHORIZED_QUEUE, true);
    }

    @Bean
    public Queue donationFailedQueue() {
        return new Queue(DONATION_FAILED_QUEUE, true);
    }

    @Bean
    public TopicExchange donationExchange() {
        return new TopicExchange(DONATION_EXCHANGE);
    }

    @Bean
    public Binding capturedBinding(Queue donationCapturedQueue, TopicExchange donationExchange) {
        return BindingBuilder.bind(donationCapturedQueue).to(donationExchange).with("donation.captured");
    }

    @Bean
    public Binding authorizedBinding(Queue donationAuthorizedQueue, TopicExchange donationExchange) {
        return BindingBuilder.bind(donationAuthorizedQueue).to(donationExchange).with("donation.authorized");
    }

    @Bean
    public Binding failedBinding(Queue donationFailedQueue, TopicExchange donationExchange) {
        return BindingBuilder.bind(donationFailedQueue).to(donationExchange).with("donation.failed");
    }

    /**
     * JSON Message Converter for deserializing messages
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
     * Configure listener container factory with JSON converter
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
