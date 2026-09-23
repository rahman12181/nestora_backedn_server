package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActionRequiredResponse {

    private Integer totalActions;
    private List<ActionItem> items;

    @Data
    @Builder
    public static class ActionItem {
        private String id;             // unique key
        private String type;           // PENDING_BOOKING, EXPIRING_SUB, UPI_MISSING, etc.
        private String priority;       // HIGH, MEDIUM, LOW
        private String title;
        private String description;
        private String actionLabel;    // "Review Now", "Renew", etc.
        private String actionRoute;    // frontend navigation hint
        private String icon;           // frontend icon hint
        private String color;          // hex color
        private Long referenceId;      // booking ID, property ID, etc.
        private Long count;            // agar multiple items hain (e.g., 3 pending bookings)
    }
}