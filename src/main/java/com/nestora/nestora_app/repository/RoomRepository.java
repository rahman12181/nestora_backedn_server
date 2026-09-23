package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.Room;
import com.nestora.nestora_app.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // ============================================
    // EXISTING
    // ============================================
    List<Room> findByProperty(Property property);

    List<Room> findByPropertyAndStatus(Property property, RoomStatus status);

    int countByPropertyAndStatus(Property property, RoomStatus status);

    // ============================================
    // Dashboard Analytics
    // ============================================
    @Query("SELECT COUNT(r) FROM Room r WHERE r.property = :property")
    Long countByProperty(@Param("property") Property property);

    @Query("SELECT COUNT(r) FROM Room r " +
            "WHERE r.property = :property AND r.status = :status")
    Long countByPropertyAndStatusLong(
            @Param("property") Property property,
            @Param("status") RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.property IN :properties")
    List<Room> findByPropertyIn(@Param("properties") List<Property> properties);

    @Query(value = "SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END) as occupied " +
            "FROM rooms WHERE property_id = :propertyId",
            nativeQuery = true)
    Object[] getOccupancyStats(@Param("propertyId") Long propertyId);

    // ============================================
    // ✅ NEW — Tenant tracking
    // ============================================
    Optional<Room> findByCurrentUserId(Long userId);

    Optional<Room> findByCurrentAgreementId(Long agreementId);

    @Query("SELECT r FROM Room r WHERE r.currentUserId = :userId")
    List<Room> findAllByCurrentUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Room r WHERE r.currentAgreementId = :agreementId")
    Optional<Room> findFirstByCurrentAgreementId(@Param("agreementId") Long agreementId);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.property = :property AND r.status = 'AVAILABLE'")
    Long countAvailableByProperty(@Param("property") Property property);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.property = :property AND r.status = 'OCCUPIED'")
    Long countOccupiedByProperty(@Param("property") Property property);
}