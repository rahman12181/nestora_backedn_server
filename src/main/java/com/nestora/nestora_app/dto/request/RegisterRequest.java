package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10 digit Indian mobile number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // NEW — Refer & Earn: optional referral code (= referrer's displayId, e.g. "NST-000042")
    private String referralCode;
}