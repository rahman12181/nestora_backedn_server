package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.CreateConversationRequest;
import com.nestora.nestora_app.dto.request.EditMessageRequest;
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

    // =============================================
    // CREATE OR GET CONVERSATION
    // =============================================
    @Transactional
    public ConversationResponse createOrGetConversation(
            User currentUser,
            CreateConversationRequest request) {

        User ownerUser = userRepository.findById(request.getOwnerUserId())
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        var existing = conversationRepository
                .findByUserAndOwnerUserAndPropertyId(
                        currentUser,
                        ownerUser,
                        request.getPropertyId()
                );

        if (existing.isPresent()) {
            return mapToConversationResponse(existing.get(), currentUser);
        }

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findById(request.getPropertyId())
                    .orElse(null);
        }

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

    // =============================================
    // GET MY CONVERSATIONS
    // =============================================
    public List<ConversationResponse> getMyConversations(User currentUser) {
        return conversationRepository
                .findByUserOrOwnerUser(currentUser, currentUser)
                .stream()
                .filter(c -> {
                    if (c.getUser().getId().equals(currentUser.getId())) {
                        return !Boolean.TRUE.equals(c.getIsUserArchived());
                    }
                    return !Boolean.TRUE.equals(c.getIsOwnerArchived());
                })
                .map(c -> mapToConversationResponse(c, currentUser))
                .sorted((a, b) -> {
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .collect(Collectors.toList());
    }

    // =============================================
    // GET MESSAGES
    // =============================================
    @Transactional
    public List<MessageResponse> getMessages(User currentUser,
                                             Long conversationId) {

        Conversation conversation = getConversationForUser(currentUser, conversationId);

        messageRepository.markAllAsRead(conversation, currentUser.getId());

        return messageRepository
                .findByConversationOrderBySentAtAsc(conversation)
                .stream()
                .filter(m -> {
                    // "Delete for me" wale messages sirf sender ko hide karo
                    if (Boolean.TRUE.equals(m.getDeletedForSenderOnly())) {
                        return !m.getSender().getId().equals(currentUser.getId());
                    }
                    return true;
                })
                .map(m -> mapToMessageResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    // =============================================
    // SEND MESSAGE
    // =============================================
    @Transactional
    public MessageResponse sendMessage(User currentUser,
                                       SendMessageRequest request) {

        Conversation conversation = getConversationForUser(
                currentUser, request.getConversationId()
        );

        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(request.getContent())
                .isRead(false)
                .isDeletedForEveryone(false)
                .deletedForSenderOnly(false)
                .isEdited(false)
                .build();

        Message saved = messageRepository.save(message);

        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageResponse response = mapToMessageResponse(saved, currentUser);

        Long receiverId = getReceiverId(conversation, currentUser);

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                response
        );

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/conversation-update",
                mapToConversationResponse(conversation, currentUser)
        );

        log.info("Message sent from {} to {} in conversation {}",
                currentUser.getId(), receiverId, conversation.getId());

        return response;
    }

    // =============================================
    // 🆕 EDIT MESSAGE
    // =============================================
    @Transactional
    public MessageResponse editMessage(User currentUser,
                                       Long messageId,
                                       EditMessageRequest request) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(
                        "Message not found", HttpStatus.NOT_FOUND
                ));

        // Sirf sender hi edit kar sakta hai
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new AppException(
                    "You can only edit your own messages",
                    HttpStatus.FORBIDDEN
            );
        }

        // Already deleted message edit nahi hoga
        if (Boolean.TRUE.equals(message.getIsDeletedForEveryone())) {
            throw new AppException(
                    "Cannot edit a deleted message",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Original content save karo (pehli baar edit pe)
        if (!Boolean.TRUE.equals(message.getIsEdited())) {
            message.setOriginalContent(message.getContent());
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message updated = messageRepository.save(message);
        MessageResponse response = mapToMessageResponse(updated, currentUser);

        // Real-time — dusre user ko edit notify karo
        Conversation conversation = message.getConversation();
        Long receiverId = getReceiverId(conversation, currentUser);

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/message-edited",
                response
        );

        log.info("Message {} edited by user {}", messageId, currentUser.getId());

        return response;
    }

    // =============================================
    // 🆕 DELETE FOR EVERYONE
    // =============================================
    @Transactional
    public MessageResponse deleteForEveryone(User currentUser, Long messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(
                        "Message not found", HttpStatus.NOT_FOUND
                ));

        // Sirf sender hi "delete for everyone" kar sakta hai
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new AppException(
                    "You can only delete your own messages for everyone",
                    HttpStatus.FORBIDDEN
            );
        }

        message.setIsDeletedForEveryone(true);
        message.setContent("This message was deleted");
        messageRepository.save(message);

        // Conversation ka lastMessage update karo
        Conversation conversation = message.getConversation();
        messageRepository.findLastActiveMessage(conversation)
                .ifPresentOrElse(
                        lastMsg -> {
                            conversation.setLastMessage(lastMsg.getContent());
                            conversation.setLastMessageAt(lastMsg.getSentAt());
                        },
                        () -> {
                            conversation.setLastMessage(null);
                            conversation.setLastMessageAt(null);
                        }
                );
        conversationRepository.save(conversation);

        MessageResponse response = mapToMessageResponse(message, currentUser);

        // Real-time — dono ko notify karo ki message delete hua
        Long receiverId = getReceiverId(conversation, currentUser);

        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/message-deleted",
                response
        );

        // Sender ko bhi confirm bhejo
        messagingTemplate.convertAndSendToUser(
                currentUser.getId().toString(),
                "/queue/message-deleted",
                response
        );

        log.info("Message {} deleted for everyone by user {}",
                messageId, currentUser.getId());

        return response;
    }

    // =============================================
    // 🆕 DELETE FOR ME (Sirf apne liye)
    // =============================================
    @Transactional
    public String deleteForMe(User currentUser, Long messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(
                        "Message not found", HttpStatus.NOT_FOUND
                ));

        // Conversation ka participant hona chahiye
        Conversation conversation = message.getConversation();
        boolean isParticipant =
                conversation.getUser().getId().equals(currentUser.getId()) ||
                        conversation.getOwnerUser().getId().equals(currentUser.getId());

        if (!isParticipant) {
            throw new AppException(
                    "Not authorized to delete this message",
                    HttpStatus.FORBIDDEN
            );
        }

        // Sirf sender ke liye delete (receiver ko dikhta rahega)
        if (message.getSender().getId().equals(currentUser.getId())) {
            message.setDeletedForSenderOnly(true);
            messageRepository.save(message);
        } else {
            // Receiver ne apne liye delete kiya — soft delete approach
            // Receiver ke liye bhi deletedForSenderOnly use karo
            // (sender = currentUser ke liye — receiver context mein)
            // Simple approach: separate field nahi chahiye — bas filter karo
            message.setDeletedForSenderOnly(true);
            messageRepository.save(message);
        }

        log.info("Message {} deleted for me by user {}",
                messageId, currentUser.getId());

        return "Message deleted for you";
    }

    // =============================================
    // MARK MESSAGES AS READ
    // =============================================
    @Transactional
    public String markMessagesAsRead(User currentUser, Long conversationId) {
        Conversation conversation = getConversationForUser(currentUser, conversationId);
        messageRepository.markAllAsRead(conversation, currentUser.getId());
        return "Messages marked as read";
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================
    private Conversation getConversationForUser(User user, Long conversationId) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(
                        "Conversation not found", HttpStatus.NOT_FOUND
                ));

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

        User otherUser;
        if (c.getUser().getId().equals(currentUser.getId())) {
            otherUser = c.getOwnerUser();
        } else {
            otherUser = c.getUser();
        }

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

    // 🆕 Updated — isMine + isEdited + isDeleted fields
    private MessageResponse mapToMessageResponse(Message message, User currentUser) {

        String displayContent;
        if (Boolean.TRUE.equals(message.getIsDeletedForEveryone())) {
            displayContent = null; // Frontend "This message was deleted" dikhayega
        } else {
            displayContent = message.getContent();
        }

        return MessageResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderPic(message.getSender().getProfilePic())
                .content(displayContent)
                .isRead(message.getIsRead())
                .sentAt(message.getSentAt())
                .isDeletedForEveryone(message.getIsDeletedForEveryone())
                .isEdited(message.getIsEdited())
                .editedAt(message.getEditedAt())
                .isMine(message.getSender().getId().equals(currentUser.getId()))
                .build();
    }
}