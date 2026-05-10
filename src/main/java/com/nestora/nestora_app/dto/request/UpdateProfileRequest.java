package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name 2-100 characters hona chahiye")
    private String name;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Valid 10 digit Indian mobile number daalo"
    )
    private String phone;
}
