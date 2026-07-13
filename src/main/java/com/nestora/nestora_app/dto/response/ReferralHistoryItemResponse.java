package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferralHistoryItemResponse {

    private String referredUserName;
    private String referredUserDisplayId;
    private String status;          // PENDING / REWARDED / REJECTED
    private BigDecimal rewardAmount;
    private LocalDateTime joinedAt;
    private LocalDateTime rewardedAt;
}