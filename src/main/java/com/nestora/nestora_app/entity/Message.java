package com.nestora.nestora_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // =============================================
    // 🆕 NAYE FIELDS — Delete & Edit feature
    // =============================================

    // Delete for everyone — dono ke liye hide
    @Column(name = "is_deleted_for_everyone")
    private Boolean isDeletedForEveryone = false;

    // Delete for me — sirf sender ke liye hide
    // Sender ka userId store karo agar usne sirf apne liye delete kiya
    @Column(name = "deleted_for_sender_only")
    private Boolean deletedForSenderOnly = false;

    // Edit — message edit hua ya nahi
    @Column(name = "is_edited")
    private Boolean isEdited = false;

    // Edit history ke liye — original content
    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (isDeletedForEveryone == null) isDeletedForEveryone = false;
        if (deletedForSenderOnly == null) deletedForSenderOnly = false;
        if (isEdited == null) isEdited = false;
    }
}