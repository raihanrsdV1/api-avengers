package com.careforall.pledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Transactional Outbox Pattern Implementation
 * This entity ensures that domain events are reliably published to RabbitMQ
 * by storing them in the same database transaction as the business entity
 * (Pledge)
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_event_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the aggregate (e.g., Pledge ID) that this event relates to
     */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /**
     * Type of event (e.g., "DONATION_CREATED", "DONATION_CAPTURED")
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * JSON payload containing event data
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the event was successfully published to RabbitMQ
     * Null means not yet published
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Number of retry attempts if publishing fails
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
}
