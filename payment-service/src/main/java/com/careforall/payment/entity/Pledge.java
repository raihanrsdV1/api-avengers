package com.careforall.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pledge Entity - Represents a donation pledge in the system
 * Implements Optimistic Locking via @Version to prevent concurrent state
 * overwrites
 */
@Entity
@Table(name = "pledges", indexes = {
        @Index(name = "idx_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PledgeStatus status;

    /**
     * Optimistic Locking - prevents concurrent updates from overwriting each other
     * Hibernate will automatically increment this on each update
     */
    @Version
    private Long version;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "payment_gateway_id", unique = true)
    private String paymentGatewayId;

    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Finite State Machine (FSM) for Pledge Status
     * Valid transitions:
     * CREATED -> AUTHORIZED -> CAPTURED
     * CREATED -> FAILED
     * AUTHORIZED -> FAILED
     */
    public enum PledgeStatus {
        CREATED, // Initial state when pledge is created
        AUTHORIZED, // Payment authorized by gateway
        CAPTURED, // Payment captured (money transferred)
        FAILED // Payment failed at any stage
    }

    /**
     * Validates if a state transition is allowed based on FSM rules
     * 
     * @param newStatus The new status to transition to
     * @return true if transition is valid, false otherwise
     */
    public boolean canTransitionTo(PledgeStatus newStatus) {
        if (this.status == newStatus) {
            return true; // Idempotent - same state is allowed
        }

        return switch (this.status) {
            case CREATED -> newStatus == PledgeStatus.AUTHORIZED ||
                    newStatus == PledgeStatus.FAILED;
            case AUTHORIZED -> newStatus == PledgeStatus.CAPTURED ||
                    newStatus == PledgeStatus.FAILED;
            case CAPTURED, FAILED -> false; // Terminal states
        };
    }
}
