package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long totalOwners;
    private Long verifiedOwners;
    private Long pendingVerifications;
    private Long totalProperties;
    private Long publishedProperties;
    private Long pendingProperties;
    private BigDecimal totalRevenue;
}
