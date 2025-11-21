package com.careforall.campaign.repository;

import com.careforall.campaign.entity.Campaign;
import com.careforall.campaign.entity.Campaign.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByCreatedBy(String createdBy);
}
