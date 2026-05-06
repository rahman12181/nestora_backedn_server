package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.RoomStatus;
import com.nestora.nestora_app.enums.RoomType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RoomResponse {
    private Long roomId;
    private String roomNumber;
    private RoomType roomType;
    private Integer floorNumber;
    private BigDecimal monthlyRent;
    private Integer capacity;
    private Integer occupiedCount;
    private RoomStatus status;
    private Boolean hasAc;
    private Boolean hasAttachedBathroom;
    private String description;
}
