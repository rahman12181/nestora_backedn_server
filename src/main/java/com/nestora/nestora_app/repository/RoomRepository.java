package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.Room;
import com.nestora.nestora_app.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByProperty(Property property);

    List<Room> findByPropertyAndStatus(Property property, RoomStatus status);

    int countByPropertyAndStatus(Property property, RoomStatus status);

    // ============================================
    // NEW — Dashboard analytics ke liye
    // ============================================

    // Total rooms count per property
    @Query("SELECT COUNT(r) FROM Room r WHERE r.property = :property")
    Long countByProperty(@Param("property") Property property);

    // Occupied rooms count per property
    @Query("SELECT COUNT(r) FROM Room r " +
            "WHERE r.property = :property AND r.status = :status")
    Long countByPropertyAndStatusLong(
            @Param("property") Property property,
            @Param("status") RoomStatus status);

    // Multiple properties ke saare rooms
    @Query("SELECT r FROM Room r WHERE r.property IN :properties")
    List<Room> findByPropertyIn(@Param("properties") List<Property> properties);

    // Occupancy percentage per property (native query for performance)
    @Query(value = "SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END) as occupied " +
            "FROM rooms WHERE property_id = :propertyId",
            nativeQuery = true)
    Object[] getOccupancyStats(@Param("propertyId") Long propertyId);
}