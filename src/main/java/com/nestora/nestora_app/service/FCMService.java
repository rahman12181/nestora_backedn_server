package com.nestora.nestora_app.service;


import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FCMService {

    // =============================================
    // Single user ko notification bhejo
    // =============================================
    public void sendNotification(String fcmToken,
                                 String title,
                                 String body,
                                 String type,
                                 String refId) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token is null — skipping push notification");
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .putData("type", type)
                    .putData("refId", refId != null ? refId : "")
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM notification sent: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed: {}", e.getMessage());
        }
    }

    // =============================================
    // Multiple users ko notification bhejo
    // =============================================
    public void sendMulticastNotification(java.util.List<String> tokens,
                                          String title,
                                          String body,
                                          String type) {
        if (tokens == null || tokens.isEmpty()) return;

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .putData("type", type)
                    .build();

            BatchResponse response = FirebaseMessaging
                    .getInstance()
                    .sendEachForMulticast(message);

            log.info("FCM multicast: {} success, {} failed",
                    response.getSuccessCount(),
                    response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast failed: {}", e.getMessage());
        }
    }
}