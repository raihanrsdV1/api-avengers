package com.careforall.campaign.controller;

import com.careforall.campaign.dto.CreateCampaignRequest;
import com.careforall.campaign.entity.Campaign;
import com.careforall.campaign.service.CampaignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Campaign Controller - Handles campaign CRUD operations
 */
@RestController
@RequestMapping("/api/v1/campaign")
@RequiredArgsConstructor
@Slf4j
public class CampaignController {

    private final CampaignService campaignService;

    /**
     * Get campaign by ID - Fast read from denormalized model
     */
    @GetMapping("/{id}")
    public ResponseEntity<Campaign> getCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = campaignService.getCampaignById(id);
            return ResponseEntity.ok(campaign);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active campaigns
     */
    @GetMapping("/active")
    public ResponseEntity<List<Campaign>> getActiveCampaigns() {
        List<Campaign> campaigns = campaignService.getActiveCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    /**
     * Create a new campaign
     */
    @PostMapping
    public ResponseEntity<Campaign> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            HttpServletRequest httpRequest) {

        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) {
            userId = "anonymous";
        }

        try {
            Campaign campaign = campaignService.createCampaign(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(campaign);
        } catch (Exception e) {
            log.error("Error creating campaign", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all campaigns (admin endpoint)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Campaign Service is running");
    }
}
