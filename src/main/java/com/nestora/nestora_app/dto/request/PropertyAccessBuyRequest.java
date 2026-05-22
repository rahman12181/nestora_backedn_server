package com.nestora.nestora_app.dto.request;

import com.nestora.nestora_app.enums.PropertyAccessPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PropertyAccessBuyRequest {

    @NotNull(message = "Plan is required")
    private PropertyAccessPlan plan;
}