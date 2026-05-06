package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    Optional<OwnerProfile> findByUser(User user);
    boolean existsByUser(User user);
    List<OwnerProfile> findByVerificationStatus(VerificationStatus status);
}