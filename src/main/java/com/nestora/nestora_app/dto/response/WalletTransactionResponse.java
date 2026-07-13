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
public class WalletTransactionResponse {

    private Long transactionId;
    private String type;        // CREDIT / DEBIT
    private BigDecimal amount;
    private String reason;      // REFERRAL_BONUS / WITHDRAWAL_HOLD / etc.
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}