package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentInvoiceRepository extends JpaRepository<RentInvoice, Long> {

    // ============================================
    // EXISTING
    // ============================================
    @Query("SELECT i FROM RentInvoice i WHERE i.agreement = :ag " +
            "ORDER BY i.dueDate DESC")
    List<RentInvoice> findByAgreementOrderByDueDateDesc(@Param("ag") RentalAgreement ag);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement.user = :user " +
            "AND i.status = 'PENDING' ORDER BY i.dueDate ASC")
    List<RentInvoice> findPendingByUser(@Param("user") User user);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement = :ag " +
            "AND i.status = 'PENDING' ORDER BY i.dueDate ASC")
    List<RentInvoice> findPendingByAgreement(@Param("ag") RentalAgreement ag);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement = :ag " +
            "AND i.status = 'PENDING' ORDER BY i.dueDate ASC LIMIT 1")
    Optional<RentInvoice> findNextPendingByAgreement(@Param("ag") RentalAgreement ag);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement.user = :user " +
            "AND i.dueDate BETWEEN :from AND :to " +
            "AND i.status = 'PENDING' ORDER BY i.dueDate ASC")
    List<RentInvoice> findUpcomingByUser(
            @Param("user") User user,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT i FROM RentInvoice i WHERE i.status = 'PENDING' " +
            "AND i.dueDate < :today")
    List<RentInvoice> findOverdue(@Param("today") LocalDate today);

    Optional<RentInvoice> findByInvoiceCode(String code);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement.user = :user " +
            "ORDER BY i.dueDate DESC")
    List<RentInvoice> findByUserOrderByDueDateDesc(@Param("user") User user);

    // ============================================
    // ✅ NEW — For display in ascending order
    // ============================================
    @Query("SELECT i FROM RentInvoice i WHERE i.agreement = :ag " +
            "ORDER BY i.dueDate ASC")
    List<RentInvoice> findByAgreementOrderByDueDateAsc(@Param("ag") RentalAgreement ag);

    @Query("SELECT i FROM RentInvoice i WHERE i.agreement.user = :user " +
            "ORDER BY i.dueDate ASC")
    List<RentInvoice> findByUserOrderByDueDateAsc(@Param("user") User user);

    // ============================================
    // ✅ NEW — For scheduler: date range
    // ============================================
    @Query("SELECT i FROM RentInvoice i WHERE i.status = 'PENDING' " +
            "AND i.dueDate BETWEEN :from AND :to")
    List<RentInvoice> findPendingByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ============================================
    // ✅ NEW — Count queries
    // ============================================
    @Query("SELECT COUNT(i) FROM RentInvoice i WHERE i.agreement = :ag " +
            "AND i.status = :status")
    Long countByAgreementAndStatus(
            @Param("ag") RentalAgreement ag,
            @Param("status") String status);

    @Query("SELECT COUNT(i) FROM RentInvoice i WHERE i.agreement.user = :user " +
            "AND i.status = 'PENDING'")
    Long countPendingByUser(@Param("user") User user);
}