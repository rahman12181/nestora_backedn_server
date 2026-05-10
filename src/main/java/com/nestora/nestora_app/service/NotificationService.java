package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.NotificationResponse;
import com.nestora.nestora_app.entity.Notification;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;

    // =============================================
    // CREATE NOTIFICATION — DB + FCM Push
    // =============================================
    public void createNotification(User user,
                                   String title,
                                   String body,
                                   NotificationType type,
                                   Long refId) {
        try {
            // DB me save karo
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .body(body)
                    .type(type)
                    .refId(refId)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);

            // FCM Push bhejo — agar token hai
            if (user.getFcmToken() != null) {
                fcmService.sendNotification(
                        user.getFcmToken(),
                        title,
                        body,
                        type.name(),
                        refId != null ? refId.toString() : null
                );
            }

        } catch (Exception e) {
            log.error("Failed to create notification: {}", e.getMessage());
        }
    }

    // =============================================
    // GET MY NOTIFICATIONS
    // =============================================
    public List<NotificationResponse> getMyNotifications(User currentUser) {
        return notificationRepository
                .findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // GET UNREAD COUNT
    // =============================================
    public long getUnreadCount(User currentUser) {
        return notificationRepository
                .countByUserAndIsReadFalse(currentUser);
    }

    // =============================================
    // MARK AS READ
    // =============================================
    @Transactional
    public String markAsRead(User currentUser, Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new AppException(
                        "Notification not found", HttpStatus.NOT_FOUND
                ));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        return "Notification marked as read";
    }

    // =============================================
    // MARK ALL AS READ
    // =============================================
    @Transactional
    public String markAllAsRead(User currentUser) {
        List<Notification> unread = notificationRepository
                .findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .filter(n -> !n.getIsRead())
                .collect(Collectors.toList());

        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        return "All notifications marked as read";
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .refId(n.getRefId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}