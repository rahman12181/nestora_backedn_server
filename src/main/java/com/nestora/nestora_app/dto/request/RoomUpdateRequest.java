package com.nestora.nestora_app.dto.request;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoomUpdateRequest {
    private String roomNumber;
    private Integer floorNumber;
    private BigDecimal monthlyRent;
    private Integer capacity;
    private Boolean hasAc;
    private Boolean hasAttachedBathroom;
    private String description;
}
