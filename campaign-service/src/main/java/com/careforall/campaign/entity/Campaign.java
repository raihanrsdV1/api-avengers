package com.careforall.campaign.entity;

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
 * Campaign Entity - CQRS Read Model
 * 
 * This entity implements the fix for the "100% CPU" problem.
 * Instead of calculating totals on-the-fly from all pledges,
 * we maintain a denormalized "currentTotalAmount" field that
 * is updated incrementally when donation events arrive.
 * 
 * This allows GET /campaigns/{id} to be a simple SELECT query
 * instead of an expensive SUM aggregation.
 */
@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "goal_amount", precision = 10, scale = 2)
    private BigDecimal goalAmount;

    /**
     * CRITICAL FIELD: This is the CQRS Read Model
     * Updated incrementally when donation events are received
     * This prevents expensive real-time aggregations
     */
    @Column(name = "current_total_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal currentTotalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.ACTIVE;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    public enum CampaignStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED
    }

    /**
     * Increment the total amount (called when donation is captured)
     */
    public void addDonation(BigDecimal amount) {
        this.currentTotalAmount = this.currentTotalAmount.add(amount);
    }
}
