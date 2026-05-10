package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingCreateRequest {

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    private Long roomId;

    @NotNull(message = "Move in date is required")
    @Future(message = "Move in date must be in future")
    private LocalDate moveInDate;

    private Integer durationMonths;

    private String message;
}
