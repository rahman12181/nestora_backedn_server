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
public class WithdrawalResponse {

    private Long withdrawalId;
    private BigDecimal amount;
    private String upiId;
    private String status;          // PENDING / APPROVED / REJECTED
    private String transactionRef;
    private String adminNote;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    // Only populated in admin list views
    private Long userId;
    private String userName;
    private String userDisplayId;
}