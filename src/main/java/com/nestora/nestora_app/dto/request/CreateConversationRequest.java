package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull(message = "Owner user ID is required")
    private Long ownerUserId;

    private Long propertyId;

    private Long bookingRequestId;
}
