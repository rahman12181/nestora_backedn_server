package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Conversation;
import com.nestora.nestora_app.entity.Message;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // ============================================
    // EXISTING (kept as-is)
    // ============================================

    // Saare messages — conversation ke order mein
    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    // Unread messages count
    long countByConversationAndIsReadFalseAndSenderIdNot(
            Conversation conversation, Long senderId
    );

    // Mark all as read
    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.conversation = :conversation
        AND m.sender.id != :userId
        AND m.isRead = false
    """)
    void markAllAsRead(
            @Param("conversation") Conversation conversation,
            @Param("userId") Long userId
    );

    // Last non-deleted message
    @Query("""
        SELECT m FROM Message m
        WHERE m.conversation = :conversation
        AND m.isDeletedForEveryone = false
        ORDER BY m.sentAt DESC
        LIMIT 1
    """)
    Optional<Message> findLastActiveMessage(
            @Param("conversation") Conversation conversation
    );

    // ============================================
    // NEW — Dashboard ke liye unread counts
    // ============================================

    // Owner ke saare conversations mein total unread messages count
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversation.ownerUser = :ownerUser
        AND m.isRead = false
        AND m.sender.id <> :ownerUserId
    """)
    Long countUnreadForOwner(
            @Param("ownerUser") User ownerUser,
            @Param("ownerUserId") Long ownerUserId
    );

    // User ke saare conversations mein total unread messages count
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversation.user = :user
        AND m.isRead = false
        AND m.sender.id <> :userId
    """)
    Long countUnreadForUser(
            @Param("user") User user,
            @Param("userId") Long userId
    );
}