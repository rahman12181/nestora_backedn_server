package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferralInfoResponse {

    private String referralCode;      // = user's displayId, e.g. "NST-000042"
    private String referralLink;      // shareable deep link
    private String shareMessage;      // ready-to-share text for Share.share()

    private Integer totalReferred;    // total friends who registered with this code
    private Integer totalRewarded;    // how many of those actually got verified & rewarded
    private BigDecimal totalEarned;   // lifetime referral earnings
    private BigDecimal walletBalance; // current withdrawable balance
    private BigDecimal rewardPerReferral; // e.g. 19.00 (shown so Flutter doesn't hardcode it)
}