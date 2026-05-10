package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    // NotNull hata do — PathVariable se set hoga
    private Long conversationId;

    @NotBlank(message = "Message content is required")
    private String content;
}