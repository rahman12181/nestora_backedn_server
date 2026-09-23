// UserActivityLogRepository.java
package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    @Query("SELECT a FROM UserActivityLog a WHERE a.user = :user " +
            "ORDER BY a.createdAt DESC")
    List<UserActivityLog> findRecentByUser(
            @Param("user") User user, Pageable pageable);

    @Query("DELETE FROM UserActivityLog a WHERE a.createdAt < :threshold")
    void deleteOlderThan(@Param("threshold") java.time.LocalDateTime threshold);
}