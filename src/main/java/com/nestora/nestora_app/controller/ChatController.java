package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.CreateConversationRequest;
import com.nestora.nestora_app.dto.request.SendMessageRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.ConversationResponse;
import com.nestora.nestora_app.dto.response.MessageResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    // REST APIs — Conversation management


    @PostMapping("/chat/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateConversationRequest request) {

        ConversationResponse response =
                chatService.createOrGetConversation(currentUser, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation started", response));
    }

    @GetMapping("/chat/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                ApiResponse.success("Conversations fetched",
                        chatService.getMyConversations(currentUser))
        );
    }

    @GetMapping("/chat/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                ApiResponse.success("Messages fetched",
                        chatService.getMessages(currentUser, conversationId))
        );
    }

    @PostMapping("/chat/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {

        request.setConversationId(conversationId);
        MessageResponse response = chatService.sendMessage(currentUser, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", response));
    }

    @PatchMapping("/chat/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        chatService.markMessagesAsRead(currentUser, conversationId))
        );
    }

    // WebSocket — Real-time message


    @MessageMapping("/chat.send")
    // Flutter se: stompClient.send("/app/chat.send", message)
    public void handleWebSocketMessage(
            @Payload SendMessageRequest request,
            Principal principal) {

        // Principal se current user nikalo
        User currentUser = (User) ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) principal).getPrincipal();

        chatService.sendMessage(currentUser, request);
    }
}
