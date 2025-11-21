package com.careforall.payment.service;

import com.careforall.payment.dto.DonationEvent;
import com.careforall.payment.dto.WebhookEvent;
import com.careforall.payment.entity.OutboxEvent;
import com.careforall.payment.entity.Pledge;
import com.careforall.payment.entity.Pledge.PledgeStatus;
import com.careforall.payment.repository.OutboxEventRepository;
import com.careforall.payment.repository.PledgeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Payment Service - Core business logic
 * Implements:
 * 1. FSM (Finite State Machine) for state transitions
 * 2. Transactional Outbox Pattern for reliable event publishing
 * 3. Optimistic Locking for concurrency control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PledgeRepository pledgeRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Process webhook from payment gateway
     * This method implements the critical fixes:
     * 
     * FIX 1: FSM State Machine - Prevents backward state transitions
     * FIX 2: Optimistic Locking - Prevents concurrent overwrites
     * FIX 3: Transactional Outbox - Ensures events are never lost
     * 
     * @param webhookEvent The webhook event from payment gateway
     * @return Updated pledge
     * @throws IllegalStateException   if state transition is invalid
     * @throws OptimisticLockException if concurrent update detected
     */
    @Transactional
    public Pledge processWebhook(WebhookEvent webhookEvent) {
        log.info("Processing webhook for payment gateway ID: {}, new status: {}",
                webhookEvent.getPaymentGatewayId(), webhookEvent.getStatus());

        // Find existing pledge by payment gateway ID
        Pledge pledge = pledgeRepository.findByPaymentGatewayId(webhookEvent.getPaymentGatewayId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pledge not found for payment gateway ID: " + webhookEvent.getPaymentGatewayId()));

        PledgeStatus currentStatus = pledge.getStatus();
        PledgeStatus newStatus = webhookEvent.getStatus();

        log.debug("Current status: {}, New status: {}, Version: {}",
                currentStatus, newStatus, pledge.getVersion());

        // ============================================================
        // CRITICAL FIX #1: FSM State Machine Validation
        // ============================================================
        // This prevents the "CAPTURED -> AUTHORIZED" backward transition bug
        // that caused negative campaign totals in the original system

        if (!pledge.canTransitionTo(newStatus)) {
            log.warn("Invalid state transition attempted: {} -> {}. Ignoring webhook.",
                    currentStatus, newStatus);
            // Return 200 OK to prevent gateway retries, but don't update state
            return pledge;
        }

        // State transition is valid, update the pledge
        pledge.setStatus(newStatus);

        // ============================================================
        // CRITICAL FIX #2: Optimistic Locking
        // ============================================================
        // The @Version field on Pledge entity ensures that if two webhooks
        // arrive simultaneously, only one will succeed. The other will throw
        // OptimisticLockException and should be retried by the gateway.

        Pledge savedPledge = pledgeRepository.save(pledge);
        log.info("Pledge {} updated to status {} (version: {})",
                savedPledge.getId(), savedPledge.getStatus(), savedPledge.getVersion());

        // ============================================================
        // CRITICAL FIX #3: Transactional Outbox Pattern
        // ============================================================
        // Instead of publishing directly to RabbitMQ (which could fail),
        // we save the event in the SAME transaction as the pledge update.
        // This guarantees that if the pledge is saved, the event will
        // eventually be published (by the OutboxPublisher background job).

        if (newStatus == PledgeStatus.CAPTURED) {
            saveOutboxEvent(savedPledge, "DONATION_CAPTURED");
        } else if (newStatus == PledgeStatus.AUTHORIZED) {
            saveOutboxEvent(savedPledge, "DONATION_AUTHORIZED");
        } else if (newStatus == PledgeStatus.FAILED) {
            saveOutboxEvent(savedPledge, "DONATION_FAILED");
        }

        // Both pledge and outbox event are committed in the same transaction
        // This is the key to preventing "lost events"
        return savedPledge;
    }

    /**
     * Create a new pledge (called when user initiates donation)
     */
    @Transactional
    public Pledge createPledge(WebhookEvent webhookEvent, String idempotencyKey) {
        log.info("Creating new pledge for campaign: {}, amount: {}",
                webhookEvent.getCampaignId(), webhookEvent.getAmount());

        Pledge pledge = Pledge.builder()
                .amount(webhookEvent.getAmount())
                .status(PledgeStatus.CREATED)
                .campaignId(webhookEvent.getCampaignId())
                .userId(webhookEvent.getUserId())
                .paymentGatewayId(webhookEvent.getPaymentGatewayId())
                .idempotencyKey(idempotencyKey)
                .build();

        Pledge savedPledge = pledgeRepository.save(pledge);

        // Save outbox event for pledge creation
        saveOutboxEvent(savedPledge, "DONATION_CREATED");

        return savedPledge;
    }

    /**
     * Save an event to the outbox table
     * This method is called within the same transaction as the pledge update
     */
    private void saveOutboxEvent(Pledge pledge, String eventType) {
        try {
            DonationEvent donationEvent = DonationEvent.builder()
                    .pledgeId(pledge.getId())
                    .campaignId(pledge.getCampaignId())
                    .userId(pledge.getUserId())
                    .amount(pledge.getAmount())
                    .status(pledge.getStatus().name())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            String payload = objectMapper.writeValueAsString(donationEvent);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(pledge.getId())
                    .eventType(eventType)
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);

            log.debug("Saved outbox event: {} for pledge: {}", eventType, pledge.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize donation event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
