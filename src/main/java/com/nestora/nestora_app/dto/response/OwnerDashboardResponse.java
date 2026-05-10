package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerDashboardResponse {
    private Long totalProperties;
    private Long publishedProperties;
    private Long totalRooms;
    private Long availableRooms;
    private Long occupiedRooms;
    private Long totalBookingRequests;
    private Long pendingRequests;
    private Long acceptedRequests;
    private Long rejectedRequests;
    private Long totalViews;
    private Double averageRating;
}
