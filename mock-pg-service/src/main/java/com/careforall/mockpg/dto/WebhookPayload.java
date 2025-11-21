package com.careforall.mockpg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Webhook payload sent to payment service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookPayload {
    private String paymentGatewayId;
    private String status; // CAPTURED or FAILED
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
    private Long pledgeId;
}
