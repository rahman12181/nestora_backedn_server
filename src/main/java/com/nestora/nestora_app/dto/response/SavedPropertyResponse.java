package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SavedPropertyResponse {
    private Long savedId;
    private Long propertyId;
    private String title;
    private PropertyType propertyType;
    private GenderAllowed genderAllowed;
    private String city;
    private String state;
    private BigDecimal monthlyRentMin;
    private BigDecimal monthlyRentMax;
    private String coverImage;
    private Integer availableRooms;
    private Boolean isVerifiedOwner;
    private LocalDateTime savedAt;
}
