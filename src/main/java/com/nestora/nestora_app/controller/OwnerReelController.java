package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.ReelResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.ReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OwnerReelController {

    private final ReelService reelService;

    // =============================================
    // OWNER — Reels
    // =============================================

    // 12.1 Upload Reel
    @PostMapping(
            value = "/owner/reels",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ReelResponse>> uploadReel(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("propertyId") Long propertyId,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestPart("file") MultipartFile file) {

        ReelResponse response = reelService.uploadReel(currentUser, propertyId, caption, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reel uploaded successfully", response));
    }

    // 12.2 Get My Reels
    @GetMapping("/owner/reels")
    public ResponseEntity<ApiResponse<List<ReelResponse>>> getMyReels(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                ApiResponse.success("Reels fetched", reelService.getMyReels(currentUser))
        );
    }

    // 12.3 Delete Reel
    @DeleteMapping("/owner/reels/{reelId}")
    public ResponseEntity<ApiResponse<String>> deleteReel(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long reelId) {

        String message = reelService.deleteReel(currentUser, reelId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}