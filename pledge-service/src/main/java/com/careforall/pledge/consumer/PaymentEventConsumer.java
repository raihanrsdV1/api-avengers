package com.careforall.pledge.consumer;

import com.careforall.pledge.config.RabbitMQConfig;
import com.careforall.pledge.dto.PaymentEvent;
import com.careforall.pledge.entity.Pledge.PledgeStatus;
import com.careforall.pledge.service.PledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Payment Event Consumer - Listens for payment status updates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PledgeService pledgeService;
    private final ObjectMapper objectMapper;

    /**
     * Handle PAYMENT_CAPTURED events
     */
    /**
     * Handle PAYMENT_CAPTURED events
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CAPTURED_QUEUE)
    public void handlePaymentCaptured(PaymentEvent event) {
        try {
            log.info("Received PAYMENT_CAPTURED event for pledge: {}", event.getPledgeId());

            pledgeService.updatePledgeStatus(
                    event.getPledgeId(),
                    event.getPaymentGatewayId(),
                    PledgeStatus.CAPTURED);

            log.info("Successfully processed PAYMENT_CAPTURED for pledge: {}", event.getPledgeId());

        } catch (Exception e) {
            log.error("Error processing PAYMENT_CAPTURED event", e);
            throw new RuntimeException("Failed to process payment captured event", e);
        }
    }

    /**
     * Handle PAYMENT_FAILED events
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentEvent event) {
        try {
            log.info("Received PAYMENT_FAILED event for pledge: {}", event.getPledgeId());

            pledgeService.updatePledgeStatus(
                    event.getPledgeId(),
                    event.getPaymentGatewayId(),
                    PledgeStatus.FAILED);

            log.info("Successfully processed PAYMENT_FAILED for pledge: {}", event.getPledgeId());

        } catch (Exception e) {
            log.error("Error processing PAYMENT_FAILED event", e);
            throw new RuntimeException("Failed to process payment failed event", e);
        }
    }
}
