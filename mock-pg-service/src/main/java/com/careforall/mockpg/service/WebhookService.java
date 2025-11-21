package com.careforall.mockpg.service;

import com.careforall.mockpg.dto.ProcessPaymentRequest;
import com.careforall.mockpg.dto.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Webhook Service - Simulates async webhook delivery to payment service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebClient.Builder webClientBuilder;
    private final Random random = new Random();

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    /**
     * Asynchronously send webhook to payment service after random delay
     * Simulates real-world payment gateway behavior
     */
    @Async
    public void sendWebhookAsync(ProcessPaymentRequest request, String paymentGatewayId) {
        try {
            // Random delay between 2-5 seconds to simulate processing time
            int delaySeconds = 2 + random.nextInt(4);
            log.info("Scheduling webhook for payment gateway ID: {} with {}s delay",
                    paymentGatewayId, delaySeconds);

            TimeUnit.SECONDS.sleep(delaySeconds);

            // 90% success rate, 10% failure rate
            String status = random.nextDouble() < 0.9 ? "CAPTURED" : "FAILED";

            WebhookPayload payload = WebhookPayload.builder()
                    .paymentGatewayId(paymentGatewayId)
                    .status(status)
                    .amount(request.getAmount())
                    .campaignId(request.getCampaignId())
                    .userId(request.getUserId())
                    .pledgeId(request.getPledgeId())
                    .build();

            log.info("Sending webhook to payment service: {}", payload);

            // Send webhook to payment service
            WebClient webClient = webClientBuilder.baseUrl(paymentServiceUrl).build();

            String response = webClient.post()
                    .uri("/api/v1/payment/webhooks/status")
                    .header("X-Idempotency-Key", "webhook-" + UUID.randomUUID())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Webhook delivered successfully. Response: {}", response);

        } catch (InterruptedException e) {
            log.error("Webhook delivery interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to deliver webhook for payment gateway ID: {}",
                    paymentGatewayId, e);
        }
    }
}
