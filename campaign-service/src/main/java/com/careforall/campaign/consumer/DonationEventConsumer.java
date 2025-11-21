package com.careforall.campaign.consumer;

import com.careforall.campaign.dto.DonationEvent;
import com.careforall.campaign.service.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Donation Event Consumer
 * 
 * Listens to the donation.captured queue and updates campaign totals
 * This completes the event-driven flow:
 * Payment Service -> RabbitMQ -> Campaign Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonationEventConsumer {

    private final CampaignService campaignService;
    private final ObjectMapper objectMapper;

    /**
     * Listen to donation.captured queue
     * When a donation is captured, update the campaign total
     */
    /**
     * Listen to donation.captured queue
     * When a donation is captured, update the campaign total
     */
    @RabbitListener(queues = com.careforall.campaign.config.RabbitMQConfig.DONATION_CAPTURED_QUEUE)
    public void handleDonationCaptured(String message) {
        try {
            log.info("Received donation.captured event: {}", message);

            // Deserialize the event
            DonationEvent event = objectMapper.readValue(message, DonationEvent.class);

            log.info("Processing donation: pledgeId={}, campaignId={}, amount={}",
                    event.getPledgeId(), event.getCampaignId(), event.getAmount());

            // Update campaign total
            campaignService.updateCampaignTotal(event.getCampaignId(), event.getAmount());

            log.info("Successfully updated campaign {} with donation {}",
                    event.getCampaignId(), event.getAmount());

        } catch (Exception e) {
            log.error("Error processing donation.captured event", e);
            // In production, you might want to send to a DLQ (Dead Letter Queue)
            throw new RuntimeException("Failed to process donation event", e);
        }
    }

    /**
     * Listen to donation.authorized queue (optional)
     * Can be used for tracking or analytics
     */
    @RabbitListener(queues = "donation.authorized.queue")
    public void handleDonationAuthorized(String message) {
        try {
            log.info("Received donation.authorized event: {}", message);
            DonationEvent event = objectMapper.readValue(message, DonationEvent.class);
            log.info("Donation authorized: pledgeId={}, campaignId={}",
                    event.getPledgeId(), event.getCampaignId());
            // Could update analytics, send notifications, etc.
        } catch (Exception e) {
            log.error("Error processing donation.authorized event", e);
        }
    }

    /**
     * Listen to donation.failed queue (optional)
     * Can be used for tracking failures or sending notifications
     */
    @RabbitListener(queues = "donation.failed.queue")
    public void handleDonationFailed(String message) {
        try {
            log.info("Received donation.failed event: {}", message);
            DonationEvent event = objectMapper.readValue(message, DonationEvent.class);
            log.warn("Donation failed: pledgeId={}, campaignId={}",
                    event.getPledgeId(), event.getCampaignId());
            // Could send notification to user, log for analytics, etc.
        } catch (Exception e) {
            log.error("Error processing donation.failed event", e);
        }
    }
}
