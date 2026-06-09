package com.nestora.nestora_app.dto.response;

import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.OccupancyStatus;
import com.nestora.nestora_app.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PropertySearchResponse {
    private Long propertyId;
    private String title;
    private PropertyType propertyType;
    private GenderAllowed genderAllowed;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal monthlyRentMin;
    private BigDecimal monthlyRentMax;
    private BigDecimal securityDeposit;
    private Boolean isNegotiable;
    private Integer totalRooms;
    private Integer availableRooms;
    private OccupancyStatus occupancyStatus;
    private String coverImage;       // Search listing ke liye — pehla image
    private List<MediaResponse> media; // 🆕 Detail page ke liye — saari images
    private List<String> amenities;
    private Boolean isVerifiedOwner;
    private Double distanceKm;
    private Double latitude;
    private Double longitude;
    private Double averageRating;
    private Integer totalReviews;
    private Boolean isFeatured;
    private Long viewCount;
    private Long ownerUserId;
    private String ownerName;
    private String ownerDisplayId;
    private String description;      // 🆕 Detail page ke liye
}