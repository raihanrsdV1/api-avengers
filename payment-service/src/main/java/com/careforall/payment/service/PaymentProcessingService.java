package com.careforall.payment.service;

import com.careforall.payment.dto.PaymentEvent;
import com.careforall.payment.dto.PledgeCreatedEvent;
import com.careforall.payment.entity.Payment;
import com.careforall.payment.entity.Payment.PaymentStatus;
import com.careforall.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Payment Processing Service - Handles payment gateway integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentGatewayService paymentGatewayService;

    @Value("${idempotency.ttl-seconds:86400}")
    private long idempotencyTtl;

    /**
     * Process payment by calling Mock Gateway
     */
    @Transactional
    public void processPayment(PledgeCreatedEvent event) {
        String idempotencyKey = "payment-" + event.getPledgeId();

        // Check idempotency
        String redisKey = "idempotency:" + idempotencyKey;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.info("Payment already processed for pledge: {}", event.getPledgeId());
            return;
        }

        // Check database idempotency
        if (paymentRepository.findByPledgeId(event.getPledgeId()).isPresent()) {
            log.info("Payment record already exists for pledge: {}", event.getPledgeId());
            return;
        }

        // Create payment record
        Payment payment = Payment.builder()
                .pledgeId(event.getPledgeId())
                .amount(event.getAmount())
                .campaignId(event.getCampaignId())
                .userId(event.getUserId())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Created payment record: {}", payment.getId());

        // Set idempotency key in Redis
        redisTemplate.opsForValue().set(redisKey, "processed", Duration.ofSeconds(idempotencyTtl));

        // Call Mock Gateway in a new transaction
        paymentGatewayService.processPaymentWithGateway(payment);
    }

    /**
     * Handle webhook from Mock Gateway
     */
    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        String paymentGatewayId = (String) webhookData.get("paymentGatewayId");
        String status = (String) webhookData.get("status");

        log.info("Processing webhook for gateway ID: {}, status: {}", paymentGatewayId, status);

        Payment payment = paymentRepository.findByPaymentGatewayId(paymentGatewayId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Payment not found for gateway ID: " + paymentGatewayId));

        PaymentStatus newStatus = "CAPTURED".equals(status) ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;
        payment.setStatus(newStatus);
        paymentRepository.save(payment);

        log.info("Payment {} updated to status: {}", payment.getId(), newStatus);

        // Publish event to RabbitMQ
        publishPaymentEvent(payment, status);
    }

    /**
     * Publish payment event to RabbitMQ
     */
    private void publishPaymentEvent(Payment payment, String status) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .pledgeId(payment.getPledgeId())
                    .paymentGatewayId(payment.getPaymentGatewayId())
                    .status(status)
                    .amount(payment.getAmount())
                    .campaignId(payment.getCampaignId())
                    .userId(payment.getUserId())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            String routingKey = "CAPTURED".equals(status) ? "payment.captured" : "payment.failed";

            rabbitTemplate.convertAndSend("pledge.exchange", routingKey, event);

            log.info("Published {} event for pledge: {}", status, payment.getPledgeId());

        } catch (Exception e) {
            log.error("Failed to publish payment event", e);
        }
    }
}
