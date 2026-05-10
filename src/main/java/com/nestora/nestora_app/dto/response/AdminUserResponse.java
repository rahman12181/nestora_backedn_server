package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {
    private Long userId;
    private String displayId;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
}
