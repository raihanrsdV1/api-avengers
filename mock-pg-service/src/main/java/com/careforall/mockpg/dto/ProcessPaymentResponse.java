package com.careforall.mockpg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from payment processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentResponse {
    private String paymentGatewayId;
    private String status; // ACCEPTED
    private String message;
}
