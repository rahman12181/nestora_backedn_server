package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.ReelCommentResponse;
import com.nestora.nestora_app.dto.response.ReelResponse;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReelService {

    // ---------- OWNER SIDE ----------
    ReelResponse uploadReel(User currentUser, Long propertyId, String caption, MultipartFile file);

    List<ReelResponse> getMyReels(User currentUser);

    String deleteReel(User currentUser, Long reelId);

    // ---------- USER / PUBLIC SIDE ----------
    // currentUser is nullable -> anonymous browsing allowed, isLikedByMe stays null
    Page<ReelResponse> getFeed(User currentUser, Pageable pageable);

    List<ReelResponse> getReelsForProperty(Long propertyId, User currentUser);

    String likeReel(User currentUser, Long reelId);

    String unlikeReel(User currentUser, Long reelId);

    ReelCommentResponse addComment(User currentUser, Long reelId, String content);

    List<ReelCommentResponse> getComments(Long reelId, User currentUser);

    String registerView(Long reelId);

    String registerShare(Long reelId);
}