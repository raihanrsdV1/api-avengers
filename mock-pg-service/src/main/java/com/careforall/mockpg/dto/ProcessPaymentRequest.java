package com.careforall.mockpg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to process a payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {
    private Long pledgeId;
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
}
