package com.nestora.nestora_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResult {
    private String payoutId;
    private String status;        // "processed" / "queued" / "pending" / "rejected" / "failed"
    private String utr;           // only present once actually processed
    private String failureReason; // only present if rejected/failed
}