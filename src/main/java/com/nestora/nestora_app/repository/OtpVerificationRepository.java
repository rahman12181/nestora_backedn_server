package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(
            String email, OtpVerification.OtpType type
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.email = :email AND o.type = :type")
    void deleteAllByEmailAndType(
            @Param("email") String email,
            @Param("type") OtpVerification.OtpType type
    );
}
