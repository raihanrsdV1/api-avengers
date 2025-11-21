package com.careforall.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayRequest {
    private Long pledgeId;
    private Double amount;
    private Long campaignId;
    private String userId;
}
