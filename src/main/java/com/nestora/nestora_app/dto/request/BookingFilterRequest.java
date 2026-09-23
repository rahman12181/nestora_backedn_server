package com.nestora.nestora_app.dto.request;

import com.nestora.nestora_app.enums.BookingStatus;
import lombok.Data;

@Data
public class BookingFilterRequest {
    private BookingStatus status;
    private Long propertyId;
    private String searchQuery;
    private String sortBy = "newest";
    private Integer page = 0;
    private Integer size = 20;
    private Boolean onlyNew = false;
    private Boolean onlyUrgent = false;
}