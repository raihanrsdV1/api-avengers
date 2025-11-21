package com.careforall.mockpg.controller;

import com.careforall.mockpg.dto.ProcessPaymentRequest;
import com.careforall.mockpg.dto.ProcessPaymentResponse;
import com.careforall.mockpg.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Payment Gateway Controller - Simulates external payment processor
 */
@RestController
@RequestMapping("/api/v1/mock-gateway")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayController {

    private final WebhookService webhookService;

    /**
     * Process payment endpoint - Returns immediate 202 ACCEPTED
     * Webhook will be sent asynchronously after 2-5 seconds
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessPaymentResponse> processPayment(
            @RequestBody ProcessPaymentRequest request) {

        log.info("Received payment processing request: {}", request);

        // Generate unique payment gateway ID
        String paymentGatewayId = "pg_" + UUID.randomUUID().toString();

        // Schedule async webhook delivery
        webhookService.sendWebhookAsync(request, paymentGatewayId);

        // Return immediate 202 ACCEPTED response
        ProcessPaymentResponse response = ProcessPaymentResponse.builder()
                .paymentGatewayId(paymentGatewayId)
                .status("ACCEPTED")
                .message("Payment processing initiated. Webhook will be sent shortly.")
                .build();

        log.info("Payment accepted with gateway ID: {}", paymentGatewayId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mock Payment Gateway is running");
    }
}
