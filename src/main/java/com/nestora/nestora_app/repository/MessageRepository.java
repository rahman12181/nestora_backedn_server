package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Conversation;
import com.nestora.nestora_app.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

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
}
