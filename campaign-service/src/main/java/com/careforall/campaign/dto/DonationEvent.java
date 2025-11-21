package com.careforall.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for donation events received from RabbitMQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationEvent {
    private Long pledgeId;
    private Long campaignId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private Long timestamp;
}
