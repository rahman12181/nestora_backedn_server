package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.OwnerActivityLog;
import com.nestora.nestora_app.entity.OwnerProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OwnerActivityLogRepository extends JpaRepository<OwnerActivityLog, Long> {

    // Recent activities with pagination
    @Query("SELECT a FROM OwnerActivityLog a " +
            "WHERE a.owner = :owner " +
            "ORDER BY a.createdAt DESC")
    List<OwnerActivityLog> findRecentByOwner(
            @Param("owner") OwnerProfile owner,
            Pageable pageable);

    // Type se filter
    @Query("SELECT a FROM OwnerActivityLog a " +
            "WHERE a.owner = :owner AND a.activityType = :type " +
            "ORDER BY a.createdAt DESC")
    List<OwnerActivityLog> findByOwnerAndType(
            @Param("owner") OwnerProfile owner,
            @Param("type") String type,
            Pageable pageable);

    // Date range
    @Query("SELECT a FROM OwnerActivityLog a " +
            "WHERE a.owner = :owner " +
            "AND a.createdAt >= :from AND a.createdAt <= :to " +
            "ORDER BY a.createdAt DESC")
    List<OwnerActivityLog> findByOwnerAndDateRange(
            @Param("owner") OwnerProfile owner,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Cleanup old logs (> 90 days)
    @Query("DELETE FROM OwnerActivityLog a WHERE a.createdAt < :threshold")
    void deleteOlderThan(@Param("threshold") LocalDateTime threshold);
}