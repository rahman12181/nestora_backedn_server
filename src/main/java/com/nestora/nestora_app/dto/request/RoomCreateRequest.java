package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.enums.RoomType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomCreateRequest {

    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    private Integer floorNumber;

    @NotNull(message = "Monthly rent is required")
    private BigDecimal monthlyRent;

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private Boolean hasAc = false;
    private Boolean hasAttachedBathroom = false;
    private String description;
}
