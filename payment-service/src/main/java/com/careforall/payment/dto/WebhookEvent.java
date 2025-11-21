package com.careforall.payment.dto;

import com.careforall.payment.entity.Pledge.PledgeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for webhook events from payment gateway
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {
    private String paymentGatewayId;
    private PledgeStatus status;
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
}
