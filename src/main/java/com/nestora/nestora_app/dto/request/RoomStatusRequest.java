package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomStatusRequest {

    @NotNull(message = "Status is required")
    private RoomStatus status;
}
