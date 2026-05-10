package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminPropertyResponse {
    private Long propertyId;
    private String title;
    private String ownerName;
    private String ownerDisplayId;
    private String city;
    private String state;
    private PropertyType propertyType;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}
