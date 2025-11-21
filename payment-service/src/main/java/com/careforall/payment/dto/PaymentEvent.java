package com.careforall.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when payment is captured or failed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    private Long pledgeId;
    private String paymentGatewayId;
    private String status; // CAPTURED or FAILED
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
    private long timestamp;
}
