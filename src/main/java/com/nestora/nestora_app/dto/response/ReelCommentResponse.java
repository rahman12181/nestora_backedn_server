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
public class ReelCommentResponse {

    private Long commentId;
    private Long userId;
    private String userName;
    private String userPic;
    private String content;
    private Boolean isMine;
    private LocalDateTime createdAt;
}