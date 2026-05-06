package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.OccupancyStatus;
import com.nestora.nestora_app.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PropertyResponse {

    private Long propertyId;
    private String title;
    private String description;
    private PropertyType propertyType;
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
    private Integer totalRooms;
    private Integer availableRooms;
    private OccupancyStatus occupancyStatus;
    private Boolean isPublished;
    private List<String> amenities;
    private List<MediaResponse> media;
    private LocalDateTime createdAt;
}