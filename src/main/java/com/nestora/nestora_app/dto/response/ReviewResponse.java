package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long reviewId;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}