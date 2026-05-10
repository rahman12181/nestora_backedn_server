package com.nestora.nestora_app.service;


import com.nestora.nestora_app.dto.request.CreateConversationRequest;
import com.nestora.nestora_app.dto.request.SendMessageRequest;
import com.nestora.nestora_app.dto.response.ConversationResponse;
import com.nestora.nestora_app.dto.response.MessageResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    // SimpMessagingTemplate — WebSocket se message bhejne ke liye

    // CREATE OR GET CONVERSATION

    @Transactional
    public ConversationResponse createOrGetConversation(
            User currentUser,
            CreateConversationRequest request) {

        User ownerUser = userRepository.findById(request.getOwnerUserId())
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        // Already conversation exist karti hai?
        var existing = conversationRepository
                .findByUserAndOwnerUserAndPropertyId(
                        currentUser,
                        ownerUser,
                        request.getPropertyId()
                );

        if (existing.isPresent()) {
            return mapToConversationResponse(existing.get(), currentUser);
        }

        // Property
        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findById(request.getPropertyId())
                    .orElse(null);
        }

        // Booking request
        BookingRequest bookingRequest = null;
        if (request.getBookingRequestId() != null) {
            bookingRequest = bookingRequestRepository
                    .findById(request.getBookingRequestId())
                    .orElse(null);
        }

        Conversation conversation = Conversation.builder()
                .user(currentUser)
                .ownerUser(ownerUser)
                .property(property)
                .bookingRequest(bookingRequest)
                .isUserArchived(false)
                .isOwnerArchived(false)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return mapToConversationResponse(saved, currentUser);
    }

    // GET MY CONVERSATIONS

    public List<ConversationResponse> getMyConversations(User currentUser) {
        return conversationRepository
                .findByUserOrOwnerUser(currentUser, currentUser)
                .stream()
                .filter(c -> {
                    // Archived conversations hide karo
                    if (c.getUser().getId().equals(currentUser.getId())) {
                        return !Boolean.TRUE.equals(c.getIsUserArchived());
                    }
                    return !Boolean.TRUE.equals(c.getIsOwnerArchived());
                })
                .map(c -> mapToConversationResponse(c, currentUser))
                .sorted((a, b) -> {
                    // Latest message wali conversation pehle
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .collect(Collectors.toList());
    }


    // GET MESSAGES

    @Transactional
    public List<MessageResponse> getMessages(User currentUser,
                                             Long conversationId) {

        Conversation conversation = getConversationForUser(
                currentUser, conversationId
        );

        // Messages padhne pe mark as read
        messageRepository.markAllAsRead(conversation, currentUser.getId());

        return messageRepository
                .findByConversationOrderBySentAtAsc(conversation)
                .stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }


    // SEND MESSAGE — REST API + WebSocket

    @Transactional
    public MessageResponse sendMessage(User currentUser,
                                       SendMessageRequest request) {

        Conversation conversation = getConversationForUser(
                currentUser, request.getConversationId()
        );

        // Message DB me save karo
        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(request.getContent())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);

        // Conversation ka last message update karo
        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageResponse response = mapToMessageResponse(saved);


        // REAL-TIME — WebSocket se dusre user ko bhejo

        Long receiverId = getReceiverId(conversation, currentUser);

        // Specific user ko message bhejo
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                response
        );

        // Conversation update bhi bhejo
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/conversation-update",
                mapToConversationResponse(conversation, currentUser)
        );

        log.info("Message sent from {} to {} in conversation {}",
                currentUser.getId(), receiverId, conversation.getId());

        return response;
    }


    // MARK MESSAGES AS READ

    @Transactional
    public String markMessagesAsRead(User currentUser, Long conversationId) {
        Conversation conversation = getConversationForUser(
                currentUser, conversationId
        );
        messageRepository.markAllAsRead(conversation, currentUser.getId());
        return "Messages marked as read";
    }


    // PRIVATE HELPERS


    private Conversation getConversationForUser(User user,
                                                Long conversationId) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(
                        "Conversation not found", HttpStatus.NOT_FOUND
                ));

        // Sirf conversation ke participants access kar sakte hain
        boolean isParticipant =
                conversation.getUser().getId().equals(user.getId()) ||
                        conversation.getOwnerUser().getId().equals(user.getId());

        if (!isParticipant) {
            throw new AppException(
                    "Not authorized to access this conversation",
                    HttpStatus.FORBIDDEN
            );
        }

        return conversation;
    }

    private Long getReceiverId(Conversation conversation, User sender) {
        if (conversation.getUser().getId().equals(sender.getId())) {
            return conversation.getOwnerUser().getId();
        }
        return conversation.getUser().getId();
    }

    private ConversationResponse mapToConversationResponse(
            Conversation c, User currentUser) {

        // Other person kaun hai
        User otherUser;
        if (c.getUser().getId().equals(currentUser.getId())) {
            otherUser = c.getOwnerUser();
        } else {
            otherUser = c.getUser();
        }

        // Unread count
        long unreadCount = messageRepository
                .countByConversationAndIsReadFalseAndSenderIdNot(
                        c, currentUser.getId()
                );

        return ConversationResponse.builder()
                .conversationId(c.getId())
                .otherUserId(otherUser.getId())
                .otherUserName(otherUser.getName())
                .otherUserPic(otherUser.getProfilePic())
                .propertyTitle(c.getProperty() != null ?
                        c.getProperty().getTitle() : null)
                .lastMessage(c.getLastMessage())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderPic(message.getSender().getProfilePic())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .sentAt(message.getSentAt())
                .build();
    }
}
