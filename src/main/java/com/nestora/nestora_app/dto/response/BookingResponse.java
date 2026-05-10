package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long requestId;
    private Long propertyId;
    private String propertyTitle;
    private String propertyCity;
    private String coverImage;
    private Long roomId;
    private String roomNumber;
    private LocalDate moveInDate;
    private Integer durationMonths;
    private String message;
    private BookingStatus status;
    private String ownerResponse;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
