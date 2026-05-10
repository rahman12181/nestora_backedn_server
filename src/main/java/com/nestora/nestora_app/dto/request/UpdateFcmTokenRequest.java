package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateFcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String fcmToken;
}
