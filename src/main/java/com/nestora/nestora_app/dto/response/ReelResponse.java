package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReelResponse {

    private Long reelId;

    private Long propertyId;
    private String propertyTitle;
    private String propertyCity;

    private Long ownerUserId;
    private String ownerName;
    private String ownerDisplayId;
    private String ownerProfilePic;
    private Boolean isVerifiedOwner;

    private String videoUrl;
    private String thumbnailUrl;
    private String caption;
    private Integer durationSec;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;

    // true/false when logged in, null when request is anonymous (unauthenticated feed browsing)
    private Boolean isLikedByMe;

    private LocalDateTime createdAt;
}