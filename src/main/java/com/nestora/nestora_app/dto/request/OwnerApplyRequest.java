package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OwnerApplyRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Aadhar number is required")
    @Pattern(
            regexp = "^[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}$",
            message = "Aadhar format: 1234 5678 9012"
    )
    private String aadharNumber;

    @NotBlank(message = "PAN number is required")
    @Pattern(
            regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
            message = "Invalid PAN. Example: ABCDE1234F"
    )
    private String panNumber;
}
