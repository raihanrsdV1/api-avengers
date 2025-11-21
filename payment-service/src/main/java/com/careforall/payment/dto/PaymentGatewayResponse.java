package com.careforall.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentGatewayResponse {
    private String paymentGatewayId;
    private String status;
    private String message;
}
