package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long userId;
    private String displayId;
    private String name;
    private String email;
    private String phone;
    private String profilePic;
    private Role role;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
}
