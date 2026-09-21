package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.RentPayment;
import com.nestora.nestora_app.enums.RentPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    Optional<RentPayment> findByBookingRequest_Id(Long bookingRequestId);

    Optional<RentPayment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<RentPayment> findByRazorpayXPayoutId(String razorpayXPayoutId);

    // used to check "is this the student's first successful paid booking" for FIRST20 eligibility
    boolean existsByStudent_IdAndStatus(Long studentId, RentPaymentStatus status);

    List<RentPayment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<RentPayment> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    List<RentPayment> findAllByOrderByCreatedAtDesc();

    List<RentPayment> findByStatusOrderByCreatedAtDesc(RentPaymentStatus status);
}