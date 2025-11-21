package com.careforall.pledge.controller;

import com.careforall.pledge.dto.CreatePledgeRequest;
import com.careforall.pledge.entity.Pledge;
import com.careforall.pledge.service.PledgeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Pledge Controller - Handles pledge creation and retrieval
 */
@RestController
@RequestMapping("/api/v1/pledge")
@RequiredArgsConstructor
@Slf4j
public class PledgeController {

    private final PledgeService pledgeService;

    /**
     * Create a new pledge
     */
    @PostMapping
    public ResponseEntity<Pledge> createPledge(
            @RequestBody CreatePledgeRequest request,
            HttpServletRequest httpRequest) {

        String idempotencyKey = httpRequest.getHeader("X-Idempotency-Key");
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        try {
            Pledge pledge = pledgeService.createPledge(request, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(pledge);

        } catch (Exception e) {
            log.error("Error creating pledge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pledge by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pledge> getPledge(@PathVariable Long id) {
        return pledgeService.getPledgeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pledge Service is running");
    }
}
