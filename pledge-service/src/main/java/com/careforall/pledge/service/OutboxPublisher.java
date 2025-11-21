package com.careforall.pledge.service;

import com.careforall.pledge.config.RabbitMQConfig;
import com.careforall.pledge.dto.PledgeCreatedEvent;
import com.careforall.pledge.entity.OutboxEvent;
import com.careforall.pledge.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Publisher - Background job that publishes events to RabbitMQ
 * 
 * This is the second part of the Transactional Outbox Pattern.
 * It polls the outbox_events table every 2 seconds and publishes
 * unpublished events to RabbitMQ.
 * 
 * KEY GUARANTEE: Events are published at-least-once
 * If publishing fails, the event stays in the table and will be retried
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("OutboxPublisher initialized and ready to publish events to RabbitMQ");
        log.info("Polling interval: ${outbox.polling.interval:2000}ms");
    }

    /**
     * Poll outbox events every 2 seconds
     * Fixed delay ensures we wait 2 seconds after the previous execution completes
     */
    @Scheduled(fixedDelayString = "${outbox.polling.interval:2000}")
    @Transactional
    public void publishOutboxEvents() {
        try {
            log.debug("OutboxPublisher polling for unpublished events...");

            // Find all unpublished events
            List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublishedEvents();

            if (unpublishedEvents.isEmpty()) {
                log.debug("No unpublished events found");
                return;
            }

            log.info("Found {} unpublished events to process", unpublishedEvents.size());

            for (OutboxEvent event : unpublishedEvents) {
                try {
                    publishEvent(event);
                } catch (Exception e) {
                    log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                    // Increment retry count
                    event.setRetryCount(event.getRetryCount() + 1);
                    outboxEventRepository.save(event);

                    // If retries exceed threshold, could add logic to move to DLQ
                    if (event.getRetryCount() > 10) {
                        log.error("Event {} exceeded max retries, manual intervention required", event.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in OutboxPublisher scheduled task: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish a single event to RabbitMQ
     */
    private void publishEvent(OutboxEvent event) {
        String routingKey = determineRoutingKey(event.getEventType());

        log.info("Publishing event {} to exchange {} with routing key {}",
                event.getId(), RabbitMQConfig.PLEDGE_EXCHANGE, routingKey);

        // Publish to RabbitMQ
        try {
            // Deserialize payload to the actual event type for proper serialization
            PledgeCreatedEvent pledgeEvent = objectMapper.readValue(
                    event.getPayload(),
                    PledgeCreatedEvent.class);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PLEDGE_EXCHANGE,
                    routingKey,
                    pledgeEvent);
        } catch (Exception e) {
            log.error("Failed to deserialize event payload: {}", event.getPayload(), e);
            throw new RuntimeException("Failed to publish event", e);
        }

        // Mark as published
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);

        log.info("Successfully published event {} (type: {})", event.getId(), event.getEventType());
    }

    /**
     * Map event type to routing key
     */
    private String determineRoutingKey(String eventType) {
        return switch (eventType) {
            case "PLEDGE_CREATED" -> RabbitMQConfig.PLEDGE_CREATED_ROUTING_KEY;
            default -> {
                log.warn("Unknown event type: {}, using default routing key", eventType);
                yield eventType.toLowerCase();
            }
        };
    }
}
