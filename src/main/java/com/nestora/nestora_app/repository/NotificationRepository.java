package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Notification;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    // ============================================
    // NEW — Dashboard ke liye unread count
    // ============================================
    @Query("SELECT COUNT(n) FROM Notification n " +
            "WHERE n.user = :user AND n.isRead = false")
    Long countUnreadByUser(@Param("user") User user);

    // Recent notifications limit ke saath
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user = :user " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUser(
            @Param("user") User user,
            org.springframework.data.domain.Pageable pageable);

    // Type se filter
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user = :user AND n.type = :type " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndType(
            @Param("user") User user,
            @Param("type") String type);
}