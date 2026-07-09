package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.ReelCommentRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.ReelCommentResponse;
import com.nestora.nestora_app.dto.response.ReelResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.ReelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReelController {

    private final ReelService reelService;

    // =============================================
    // USER / PUBLIC — Reels Feed & Engagement
    // =============================================

    // 12.4 Get Reels Feed (Public — token optional, enables isLikedByMe)
    @GetMapping("/reels/feed")
    public ResponseEntity<ApiResponse<Page<ReelResponse>>> getFeed(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ReelResponse> feed = reelService.getFeed(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reels fetched", feed));
    }

    // 12.5 Get Reels for a Property (Public)
    @GetMapping("/properties/{propertyId}/reels")
    public ResponseEntity<ApiResponse<List<ReelResponse>>> getReelsForProperty(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {

        List<ReelResponse> reels = reelService.getReelsForProperty(propertyId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reels fetched", reels));
    }

    // 12.6 Like a Reel (Auth required)
    @PostMapping("/reels/{reelId}/like")
    public ResponseEntity<ApiResponse<String>> likeReel(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long reelId) {

        String message = reelService.likeReel(currentUser, reelId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 12.7 Unlike a Reel (Auth required)
    @DeleteMapping("/reels/{reelId}/like")
    public ResponseEntity<ApiResponse<String>> unlikeReel(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long reelId) {

        String message = reelService.unlikeReel(currentUser, reelId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 12.8 Add Comment (Auth required)
    @PostMapping("/reels/{reelId}/comments")
    public ResponseEntity<ApiResponse<ReelCommentResponse>> addComment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long reelId,
            @Valid @RequestBody ReelCommentRequest request) {

        ReelCommentResponse comment = reelService.addComment(currentUser, reelId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", comment));
    }

    // 12.9 Get Comments (Public)
    @GetMapping("/reels/{reelId}/comments")
    public ResponseEntity<ApiResponse<List<ReelCommentResponse>>> getComments(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long reelId) {

        List<ReelCommentResponse> comments = reelService.getComments(reelId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Comments fetched", comments));
    }

    // 12.10 Register a View (Public — call once when a reel plays 2+ sec)
    @PatchMapping("/reels/{reelId}/view")
    public ResponseEntity<ApiResponse<String>> registerView(@PathVariable Long reelId) {
        String message = reelService.registerView(reelId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 12.11 Register a Share (Public — call when native share sheet is opened)
    @PostMapping("/reels/{reelId}/share")
    public ResponseEntity<ApiResponse<String>> registerShare(@PathVariable Long reelId) {
        String message = reelService.registerShare(reelId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}