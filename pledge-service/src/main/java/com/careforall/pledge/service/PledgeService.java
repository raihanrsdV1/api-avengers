package com.careforall.pledge.service;

import com.careforall.pledge.dto.CreatePledgeRequest;
import com.careforall.pledge.dto.PledgeCreatedEvent;
import com.careforall.pledge.entity.OutboxEvent;
import com.careforall.pledge.entity.Pledge;
import com.careforall.pledge.entity.Pledge.PledgeStatus;
import com.careforall.pledge.repository.OutboxEventRepository;
import com.careforall.pledge.repository.PledgeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Pledge Service - Manages pledge lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PledgeService {

    private final PledgeRepository pledgeRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new pledge
     */
    @Transactional
    public Pledge createPledge(CreatePledgeRequest request, String idempotencyKey) {
        log.info("Creating pledge for campaign: {}, amount: {}", request.getCampaignId(), request.getAmount());

        // Check idempotency
        Optional<Pledge> existing = pledgeRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Pledge already exists for idempotency key: {}", idempotencyKey);
            return existing.get();
        }

        // Create pledge
        Pledge pledge = Pledge.builder()
                .amount(request.getAmount())
                .status(PledgeStatus.CREATED)
                .campaignId(request.getCampaignId())
                .userId(request.getUserId())
                .idempotencyKey(idempotencyKey)
                .build();

        Pledge savedPledge = pledgeRepository.save(pledge);
        log.info("Pledge created with ID: {}", savedPledge.getId());

        // Save outbox event for PLEDGE_CREATED
        saveOutboxEvent(savedPledge, "PLEDGE_CREATED");

        return savedPledge;
    }

    /**
     * Update pledge status based on payment result
     */
    @Transactional
    public Pledge updatePledgeStatus(Long pledgeId, String paymentGatewayId, PledgeStatus newStatus) {
        log.info("Updating pledge {} to status: {}", pledgeId, newStatus);

        Pledge pledge = pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + pledgeId));

        // Set payment gateway ID if not already set
        if (pledge.getPaymentGatewayId() == null && paymentGatewayId != null) {
            pledge.setPaymentGatewayId(paymentGatewayId);
        }

        // Validate state transition
        if (!pledge.canTransitionTo(newStatus)) {
            log.warn("Invalid state transition: {} -> {}. Ignoring update.", pledge.getStatus(), newStatus);
            return pledge;
        }

        pledge.setStatus(newStatus);
        Pledge updatedPledge = pledgeRepository.save(pledge);

        log.info("Pledge {} updated to status: {}", pledgeId, newStatus);
        return updatedPledge;
    }

    /**
     * Get pledge by ID
     */
    public Optional<Pledge> getPledgeById(Long id) {
        return pledgeRepository.findById(id);
    }

    /**
     * Save outbox event for reliable event publishing
     */
    private void saveOutboxEvent(Pledge pledge, String eventType) {
        try {
            PledgeCreatedEvent event = PledgeCreatedEvent.builder()
                    .pledgeId(pledge.getId())
                    .campaignId(pledge.getCampaignId())
                    .userId(pledge.getUserId())
                    .amount(pledge.getAmount())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(pledge.getId())
                    .eventType(eventType)
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Saved outbox event: {} for pledge: {}", eventType, pledge.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize pledge event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
