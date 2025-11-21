package com.careforall.pledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to create a new pledge
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePledgeRequest {
    private BigDecimal amount;
    private Long campaignId;
    private String userId;
}
