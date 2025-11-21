package com.careforall.payment.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Idempotency Filter - Prevents duplicate processing of requests
 * 
 * This filter solves the "Charged Twice" problem from the original system.
 * It uses Redis to cache responses based on the X-Idempotency-Key header.
 * 
 * How it works:
 * 1. Client sends X-Idempotency-Key header with each request
 * 2. Filter checks if key exists in Redis
 * 3. If exists, return cached response (no processing)
 * 4. If not exists, process request and cache response
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter implements Filter {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_KEY_HEADER);

        // Only apply idempotency to POST requests with idempotency key
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod()) || idempotencyKey == null) {
            chain.doFilter(request, response);
            return;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        // Check if we've seen this request before
        String cachedResponse = redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {
            log.info("Idempotency key {} found in cache. Returning cached response.", idempotencyKey);

            // Return cached response without processing
            httpResponse.setContentType("application/json");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.getWriter().write(cachedResponse);
            return;
        }

        // Wrap request and response to cache content
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        try {
            // Process the request
            chain.doFilter(requestWrapper, responseWrapper);

            // Cache the response if status is 2xx
            int status = responseWrapper.getStatus();
            if (status >= 200 && status < 300) {
                String responseBody = new String(responseWrapper.getContentAsByteArray(),
                        StandardCharsets.UTF_8);

                // Store in Redis with TTL
                redisTemplate.opsForValue().set(redisKey, responseBody, ttlSeconds, TimeUnit.SECONDS);

                log.info("Cached response for idempotency key: {} (TTL: {} seconds)",
                        idempotencyKey, ttlSeconds);
            }

            // Copy response back to original response
            responseWrapper.copyBodyToResponse();

        } catch (Exception e) {
            log.error("Error in idempotency filter", e);
            throw e;
        }
    }
}
