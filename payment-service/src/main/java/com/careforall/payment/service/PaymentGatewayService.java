package com.careforall.payment.service;

import com.careforall.payment.dto.PaymentGatewayRequest;
import com.careforall.payment.dto.PaymentGatewayResponse;
import com.careforall.payment.entity.Payment;
import com.careforall.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final WebClient.Builder webClientBuilder;
    private final PaymentRepository paymentRepository;

    @Value("${mock.gateway.url:http://mock-pg-service:8084}")
    private String mockGatewayUrl;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentWithGateway(Payment payment) {
        try {
            PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                    .pledgeId(payment.getPledgeId())
                    .amount(payment.getAmount().doubleValue())
                    .campaignId(payment.getCampaignId())
                    .userId(payment.getUserId())
                    .build();

            WebClient webClient = webClientBuilder.baseUrl(mockGatewayUrl).build();

            PaymentGatewayResponse response = webClient.post()
                    .uri("/api/v1/mock-gateway/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentGatewayResponse.class)
                    .block();

            if (response != null && response.getPaymentGatewayId() != null) {
                payment.setPaymentGatewayId(response.getPaymentGatewayId());
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                log.info("Payment sent to gateway. Gateway ID: {}", response.getPaymentGatewayId());
            }
        } catch (Exception e) {
            log.error("Failed to call Mock Gateway for pledge: {}", payment.getPledgeId(), e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            // You might want to publish a failure event here
        }
    }
}
