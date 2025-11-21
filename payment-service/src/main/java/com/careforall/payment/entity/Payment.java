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
 * Payment Entity - Tracks payment gateway interactions
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_pledge_id", columnList = "pledge_id"),
        @Index(name = "idx_payment_gateway_id", columnList = "payment_gateway_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pledge_id", nullable = false)
    private Long pledgeId;

    @Column(name = "payment_gateway_id", unique = true)
    private String paymentGatewayId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, // Initial state
        PROCESSING, // Sent to gateway
        CAPTURED, // Successfully captured
        FAILED // Failed
    }
}
