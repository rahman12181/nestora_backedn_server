package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.TypingEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEventDto event) {

        messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getReceiverId()),
                "/queue/typing",
                Map.of(
                        "conversationId", event.getConversationId(),
                        "senderId", event.getSenderId(),
                        "isTyping", event.isTyping()
                )
        );

    }

}