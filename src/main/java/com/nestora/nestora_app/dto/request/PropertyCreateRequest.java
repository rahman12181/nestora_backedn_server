package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Gender allowed is required")
    private GenderAllowed genderAllowed;

    @NotBlank(message = "Address is required")
    private String addressLine;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private BigDecimal monthlyRentMin;
    private BigDecimal monthlyRentMax;
    private BigDecimal securityDeposit;

    private Boolean isNegotiable = false;

    private List<String> amenities;
}