package com.careforall.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentGatewayRequest {
    private Long pledgeId;
    private Double amount;
    private Long campaignId;
    private Long userId;
}
