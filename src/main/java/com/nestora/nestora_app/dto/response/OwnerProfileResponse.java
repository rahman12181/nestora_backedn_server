package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.SubscriptionPlan;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OwnerProfileResponse {
    private Long ownerId;
    private Long userId;
    private String displayId;
    private String name;
    private String email;
    private String phone;
    private String businessName;
    private String aadharNumber;
    private String panNumber;
    private String aadharDocUrl;
    private String panDocUrl;
    private String addressProofUrl;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private SubscriptionPlan subscriptionPlan;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionEnd;
    private BigDecimal monthlyFee;
    private LocalDateTime createdAt;
}