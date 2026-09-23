package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.MonthlyRentPayment;
import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.RentInvoice;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MonthlyRentPaymentRepository
        extends JpaRepository<MonthlyRentPayment, Long> {

    @Query("SELECT p FROM MonthlyRentPayment p WHERE p.student = :student " +
            "ORDER BY p.createdAt DESC")
    List<MonthlyRentPayment> findByStudentOrderByCreatedAtDesc(
            @Param("student") User student);

    @Query("SELECT p FROM MonthlyRentPayment p WHERE p.owner = :owner " +
            "ORDER BY p.createdAt DESC")
    List<MonthlyRentPayment> findByOwnerOrderByCreatedAtDesc(
            @Param("owner") OwnerProfile owner);

    Optional<MonthlyRentPayment> findByRazorpayOrderId(String orderId);

    Optional<MonthlyRentPayment> findByPaymentCode(String code);

    List<MonthlyRentPayment> findByInvoice(RentInvoice invoice);

    @Query("SELECT COUNT(p) > 0 FROM MonthlyRentPayment p " +
            "WHERE p.student.id = :studentId AND p.status = 'PAID'")
    boolean existsPaidByStudent(@Param("studentId") Long studentId);
}