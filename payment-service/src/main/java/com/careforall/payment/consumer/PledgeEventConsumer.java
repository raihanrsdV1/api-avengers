package com.careforall.payment.consumer;

import com.careforall.payment.config.RabbitMQConfig;
import com.careforall.payment.dto.PledgeCreatedEvent;
import com.careforall.payment.service.PaymentProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Pledge Event Consumer - Listens for pledge creation events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PledgeEventConsumer {

    private final PaymentProcessingService paymentProcessingService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("PledgeEventConsumer initialized and listening for PLEDGE_CREATED events on queue: {}",
                RabbitMQConfig.PLEDGE_CREATED_QUEUE);
    }

    /**
     * Handle PLEDGE_CREATED events
     */
    @RabbitListener(
            queues = RabbitMQConfig.PLEDGE_CREATED_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePledgeCreated(PledgeCreatedEvent event) {
        try {
            log.info("Received PLEDGE_CREATED event for pledge: {} with amount: {}",
                    event.getPledgeId(), event.getAmount());

            // Process payment via Mock Gateway
            paymentProcessingService.processPayment(event);

            log.info("Successfully initiated payment processing for pledge: {}", event.getPledgeId());

        } catch (Exception e) {
            log.error("Error processing PLEDGE_CREATED event for pledge: {}",
                    event != null ? event.getPledgeId() : "null", e);
            throw new RuntimeException("Failed to process pledge created event", e);
        }
    }
}
