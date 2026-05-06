package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String role;
    private Long userId;
    private String displayId;
    private String name;
    private String email;
    private Boolean isEmailVerified;
}
