package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.enums.GenderAllowed;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyUpdateRequest {

    private String title;
    private String description;
    private GenderAllowed genderAllowed;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal monthlyRentMin;
    private BigDecimal monthlyRentMax;
    private BigDecimal securityDeposit;
    private Boolean isNegotiable;
    private List<String> amenities;
}