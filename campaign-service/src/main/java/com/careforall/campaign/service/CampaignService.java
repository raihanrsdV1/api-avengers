package com.careforall.campaign.service;

import com.careforall.campaign.dto.CreateCampaignRequest;
import com.careforall.campaign.entity.Campaign;
import com.careforall.campaign.entity.Campaign.CampaignStatus;
import com.careforall.campaign.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Campaign Service - CQRS Read Model Implementation
 * 
 * This service maintains the denormalized view of campaign totals.
 * The key insight: we DON'T calculate totals on read - we update them on write.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;

    /**
     * Get campaign by ID - Simple SELECT query
     * No expensive aggregations or joins
     * This is the fix for the "100% CPU" problem
     */
    @Transactional(readOnly = true)
    public Campaign getCampaignById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
    }

    /**
     * Get all active campaigns
     */
    @Transactional(readOnly = true)
    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findByStatus(CampaignStatus.ACTIVE);
    }

    /**
     * Create a new campaign
     */
    @Transactional
    public Campaign createCampaign(CreateCampaignRequest request, String userId) {
        log.info("Creating campaign: {} by user: {}", request.getName(), userId);

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .goalAmount(request.getGoalAmount())
                .currentTotalAmount(BigDecimal.ZERO)
                .status(CampaignStatus.ACTIVE)
                .createdBy(userId)
                .endDate(request.getEndDate())
                .build();

        return campaignRepository.save(campaign);
    }

    /**
     * Update campaign total when a donation is captured
     * This is called by the event listener when DONATION_CAPTURED event arrives
     */
    @Transactional
    public void updateCampaignTotal(Long campaignId, BigDecimal amount) {
        log.info("Updating campaign {} total by {}", campaignId, amount);

        Campaign campaign = getCampaignById(campaignId);
        campaign.addDonation(amount);
        campaignRepository.save(campaign);

        log.info("Campaign {} new total: {}", campaignId, campaign.getCurrentTotalAmount());
    }

    /**
     * Get all campaigns (for admin)
     */
    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }
}
