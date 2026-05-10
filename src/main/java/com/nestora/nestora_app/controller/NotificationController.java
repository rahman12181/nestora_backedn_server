package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.NotificationResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Notifications fetched",
                        notificationService.getMyNotifications(currentUser))
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(
                ApiResponse.success("Unread count fetched",
                        Map.of("unreadCount", count))
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.markAsRead(currentUser, notificationId))
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.markAllAsRead(currentUser))
        );
    }
}
