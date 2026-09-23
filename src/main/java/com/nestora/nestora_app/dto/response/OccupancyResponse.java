package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OccupancyResponse {

    // ============ OVERALL ============
    private Integer totalRooms;
    private Integer occupiedRooms;
    private Integer availableRooms;
    private Integer maintenanceRooms;

    // ============ RATE ============
    private Double occupancyRate;        // percentage 0-100
    private String occupancyLabel;       // "Excellent", "Good", "Needs Attention"
    private Double lastWeekRate;
    private Double changeFromLastWeek;   // +2.5 or -3.0

    // ============ PER PROPERTY ============
    private List<PropertyOccupancy> byProperty;

    @Data
    @Builder
    public static class PropertyOccupancy {
        private Long propertyId;
        private String propertyTitle;
        private Integer totalRooms;
        private Integer occupiedRooms;
        private Integer availableRooms;
        private Double occupancyRate;
        private String color;            // for chart
    }
}