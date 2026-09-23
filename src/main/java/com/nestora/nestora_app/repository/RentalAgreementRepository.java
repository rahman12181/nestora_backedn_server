package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long> {

    // ============================================
    // EXISTING
    // ============================================
    @Query("SELECT a FROM RentalAgreement a WHERE a.user = :user " +
            "ORDER BY a.startDate DESC")
    List<RentalAgreement> findByUserOrderByStartDateDesc(@Param("user") User user);

    @Query("SELECT a FROM RentalAgreement a WHERE a.user = :user " +
            "AND a.status = 'ACTIVE' ORDER BY a.startDate DESC")
    Optional<RentalAgreement> findActiveByUser(@Param("user") User user);

    @Query("SELECT a FROM RentalAgreement a WHERE a.owner = :owner " +
            "AND a.status = 'ACTIVE' ORDER BY a.startDate DESC")
    List<RentalAgreement> findActiveByOwner(@Param("owner") OwnerProfile owner);

    @Query("SELECT a FROM RentalAgreement a WHERE a.status = 'ACTIVE'")
    List<RentalAgreement> findAllActive();

    Optional<RentalAgreement> findByAgreementCode(String code);

    // ============================================
    // ✅ NEW — Additional queries
    // ============================================
    @Query("SELECT a FROM RentalAgreement a WHERE a.user = :user " +
            "AND a.status = :status ORDER BY a.startDate DESC")
    Optional<RentalAgreement> findByUserAndStatus(
            @Param("user") User user,
            @Param("status") String status);

    Optional<RentalAgreement> findByRoomAndStatus(Room room, String status);

    @Query("SELECT a FROM RentalAgreement a WHERE a.room = :room " +
            "AND a.status = 'ACTIVE'")
    Optional<RentalAgreement> findActiveByRoom(@Param("room") Room room);

    @Query("SELECT COUNT(a) FROM RentalAgreement a WHERE a.owner = :owner " +
            "AND a.status = 'ACTIVE'")
    Long countActiveByOwner(@Param("owner") OwnerProfile owner);
}