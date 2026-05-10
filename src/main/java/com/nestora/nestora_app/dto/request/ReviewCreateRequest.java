package com.nestora.nestora_app.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating minimum 1 hona chahiye")
    @Max(value = 5, message = "Rating maximum 5 ho sakta hai")
    private Integer rating;

    private String comment;
}
