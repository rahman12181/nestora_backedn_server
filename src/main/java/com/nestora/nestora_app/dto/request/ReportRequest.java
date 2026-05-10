package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {

    @NotNull(message = "Type is required")
    private Report.ReportType type;

    @NotNull(message = "Reference ID is required")
    private Long refId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;
}