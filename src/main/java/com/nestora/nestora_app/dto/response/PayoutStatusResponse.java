package com.nestora.nestora_app.dto.response;

import com.nestora.nestora_app.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutStatusResponse {
    private boolean hasPayoutUpi;
    private String upiId;
    private PayoutStatus payoutStatus;
    private String lastPayoutDate;
    private Double lastPayoutAmount;
    private String message;
}