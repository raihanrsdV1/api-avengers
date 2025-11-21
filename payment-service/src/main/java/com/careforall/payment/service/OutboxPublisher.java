package com.careforall.payment.service;

import com.careforall.payment.config.RabbitMQConfig;
import com.careforall.payment.entity.OutboxEvent;
import com.careforall.payment.repository.OutboxEventRepository;
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

    /**
     * Poll outbox events every 2 seconds
     * Fixed delay ensures we wait 2 seconds after the previous execution completes
     */
    @Scheduled(fixedDelayString = "${outbox.polling.interval:2000}")
    @Transactional
    public void publishOutboxEvents() {
        // Find all unpublished events
        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublishedEvents();

        if (unpublishedEvents.isEmpty()) {
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
    }

    /**
     * Publish a single event to RabbitMQ
     */
    private void publishEvent(OutboxEvent event) {
        String routingKey = determineRoutingKey(event.getEventType());

        log.debug("Publishing event {} to exchange {} with routing key {}",
                event.getId(), RabbitMQConfig.DONATION_EXCHANGE, routingKey);

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DONATION_EXCHANGE,
                routingKey,
                event.getPayload());

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
            case "DONATION_CAPTURED" -> RabbitMQConfig.DONATION_CAPTURED_ROUTING_KEY;
            case "DONATION_AUTHORIZED" -> RabbitMQConfig.DONATION_AUTHORIZED_ROUTING_KEY;
            case "DONATION_FAILED" -> RabbitMQConfig.DONATION_FAILED_ROUTING_KEY;
            default -> {
                log.warn("Unknown event type: {}, using default routing key", eventType);
                yield eventType.toLowerCase();
            }
        };
    }
}
