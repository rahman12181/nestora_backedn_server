package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopPropertyResponse {

    private Long propertyId;
    private String title;
    private String city;
    private String coverImage;

    private BigDecimal revenueThisMonth;
    private Integer bookingsThisMonth;
    private Double averageRating;
    private Long totalReviews;
    private Long viewCount;

    private Double occupancyRate;
    private Integer availableRooms;
    private Integer totalRooms;

    // Why it's top
    private String topReason;      // "Highest revenue", "Most bookings", etc.
    private String rankBadge;      // "🏆 #1 Performer"
}