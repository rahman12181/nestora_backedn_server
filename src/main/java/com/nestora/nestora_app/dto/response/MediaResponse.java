package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.MediaType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaResponse {
    private Long mediaId;
    private MediaType mediaType;
    private String url;
    private String thumbnailUrl;
    private Integer durationSec;
    private Boolean isPrimary;
    private Integer sortOrder;
}
