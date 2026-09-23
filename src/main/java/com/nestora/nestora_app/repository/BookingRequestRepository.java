package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.BookingRequest;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    // ============ BASIC (existing, kept for compatibility) ============
    List<BookingRequest> findByUser(User user);

    List<BookingRequest> findByProperty(Property property);

    List<BookingRequest> findByPropertyIn(List<Property> properties);

    // ============ OPTIMIZED — OWNER LIST (with eager loading) ============
    @EntityGraph(attributePaths = {"user", "property", "room"})
    @Query("SELECT b FROM BookingRequest b " +
            "WHERE b.property IN :properties " +
            "ORDER BY b.requestedAt DESC")
    List<BookingRequest> findByPropertyInWithDetails(@Param("properties") List<Property> properties);

    @EntityGraph(attributePaths = {"user", "property", "room"})
    @Query("SELECT b FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.status = :status " +
            "ORDER BY b.requestedAt DESC")
    List<BookingRequest> findByPropertyInAndStatus(
            @Param("properties") List<Property> properties,
            @Param("status") BookingStatus status);

    @EntityGraph(attributePaths = {"user", "property", "room"})
    @Query("SELECT b FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.property.id = :propertyId " +
            "ORDER BY b.requestedAt DESC")
    List<BookingRequest> findByPropertyInAndPropertyId(
            @Param("properties") List<Property> properties,
            @Param("propertyId") Long propertyId);

    @EntityGraph(attributePaths = {"user", "property", "room"})
    @Query("SELECT b FROM BookingRequest b " +
            "WHERE b.property IN :properties " +
            "AND (LOWER(b.user.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "     OR LOWER(b.user.displayId) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "     OR LOWER(b.user.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY b.requestedAt DESC")
    List<BookingRequest> searchByPropertyIn(
            @Param("properties") List<Property> properties,
            @Param("query") String query);

    // ============ SINGLE FETCH WITH DETAILS ============
    @EntityGraph(attributePaths = {"user", "property", "room"})
    @Query("SELECT b FROM BookingRequest b WHERE b.id = :id")
    Optional<BookingRequest> findByIdWithDetails(@Param("id") Long id);

    // ============ ANALYTICS ============
    @Query("SELECT COUNT(b) FROM BookingRequest b WHERE b.property IN :properties")
    Long countByProperties(@Param("properties") List<Property> properties);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.status = :status")
    Long countByPropertiesAndStatus(
            @Param("properties") List<Property> properties,
            @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.status = :status " +
            "AND b.requestedAt >= :from AND b.requestedAt <= :to")
    Long countByPropertiesAndStatusBetween(
            @Param("properties") List<Property> properties,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.requestedAt >= :since")
    Long countRecent(
            @Param("properties") List<Property> properties,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.status = 'PENDING' " +
            "AND b.requestedAt <= :threshold")
    Long countUrgentPending(
            @Param("properties") List<Property> properties,
            @Param("threshold") LocalDateTime threshold);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, b.requested_at, b.responded_at)) / 60.0 " +
            "FROM booking_requests b " +
            "WHERE b.property_id IN :propertyIds " +
            "AND b.responded_at IS NOT NULL",
            nativeQuery = true)
    Double avgResponseHoursByPropertyIds(@Param("propertyIds") List<Long> propertyIds);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.property IN :properties AND b.status = 'ACCEPTED' " +
            "AND b.respondedAt >= :since")
    Long countAcceptedSince(
            @Param("properties") List<Property> properties,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(b) FROM BookingRequest b WHERE b.user = :user")
    Long countByUser(@Param("user") User user);

    @Query("SELECT COUNT(b) FROM BookingRequest b " +
            "WHERE b.user = :user AND b.status = 'ACCEPTED'")
    Long countAcceptedByUser(@Param("user") User user);

    @Query("SELECT COUNT(b) > 0 FROM BookingRequest b " +
            "WHERE b.user = :user AND b.status = 'ACCEPTED' AND b.id <> :excludeId")
    Boolean hasActiveBookingExcluding(
            @Param("user") User user,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(b) > 0 FROM BookingRequest b " +
            "WHERE b.user = :user AND b.property IN :properties AND b.id <> :excludeId")
    Boolean hasBookedOwnerBefore(
            @Param("user") User user,
            @Param("properties") List<Property> properties,
            @Param("excludeId") Long excludeId);

    // ============ SCHEDULER ============
    @Query("SELECT b FROM BookingRequest b " +
            "WHERE b.status = 'PENDING' AND b.requestedAt <= :threshold")
    List<BookingRequest> findOldPendingBookings(@Param("threshold") LocalDateTime threshold);
}