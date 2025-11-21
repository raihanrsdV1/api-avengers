package com.careforall.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event payload for donation events published to RabbitMQ
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
