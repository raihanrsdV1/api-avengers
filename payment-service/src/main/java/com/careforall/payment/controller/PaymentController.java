package com.careforall.payment.controller;

import com.careforall.payment.entity.Payment;
import com.careforall.payment.repository.PaymentRepository;
import com.careforall.payment.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment Controller - Handles webhook requests from Mock Payment Gateway
 */
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentProcessingService paymentProcessingService;
    private final PaymentRepository paymentRepository;

    /**
     * Webhook endpoint for Mock Gateway status notifications
     */
    @PostMapping("/webhooks/status")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            log.info("Received webhook from Mock Gateway: {}", webhookData);

            paymentProcessingService.handleWebhook(webhookData);

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (IllegalArgumentException e) {
            log.error("Invalid webhook data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get payment by pledge ID
     */
    @GetMapping("/pledge/{pledgeId}")
    public ResponseEntity<Payment> getPaymentByPledgeId(@PathVariable Long pledgeId) {
        return paymentRepository.findByPledgeId(pledgeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running");
    }
}
