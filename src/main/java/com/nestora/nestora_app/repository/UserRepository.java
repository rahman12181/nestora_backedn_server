package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // NEW — Refer & Earn: resolves a referral code (= the referrer's displayId) to a User
    Optional<User> findByDisplayId(String displayId);
}