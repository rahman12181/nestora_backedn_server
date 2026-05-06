package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VerificationStatusResponse {
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
}
