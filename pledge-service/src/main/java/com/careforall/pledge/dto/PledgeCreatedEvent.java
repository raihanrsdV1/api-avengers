package com.careforall.pledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when pledge is created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PledgeCreatedEvent {
    private Long pledgeId;
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
    private long timestamp;
}
