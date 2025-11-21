package com.careforall.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Gateway Route Configuration
 * 
 * Defines routes to backend services using programmatic configuration
 * This allows for more dynamic routing with environment variables
 */
@Configuration
public class GatewayConfig {

        @Value("${PAYMENT_SERVICE_URL:http://localhost:8081}")
        private String paymentServiceUrl;

        @Value("${CAMPAIGN_SERVICE_URL:http://localhost:8082}")
        private String campaignServiceUrl;

        @Value("${PLEDGE_SERVICE_URL:http://localhost:8083}")
        private String pledgeServiceUrl;

        @Value("${MOCK_PG_SERVICE_URL:http://localhost:8084}")
        private String mockPgServiceUrl;

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                // Payment Service Route
                                .route("payment-service", r -> r
                                                .path("/api/v1/payment/**")
                                                .uri(paymentServiceUrl))

                                // Campaign Service Route
                                .route("campaign-service", r -> r
                                                .path("/api/v1/campaign/**")
                                                .uri(campaignServiceUrl))

                                // Pledge Service Route
                                .route("pledge-service", r -> r
                                                .path("/api/v1/pledge/**")
                                                .uri(pledgeServiceUrl))

                                // Mock Payment Gateway Route
                                .route("mock-pg-service", r -> r
                                                .path("/api/v1/mock-gateway/**")
                                                .uri(mockPgServiceUrl))

                                .build();
        }
}
