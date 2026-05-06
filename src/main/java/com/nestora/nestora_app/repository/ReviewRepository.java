package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.Review;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPropertyAndIsVisibleTrue(Property property);
    Optional<Review> findByUserAndProperty(User user, Property property);
    boolean existsByUserAndProperty(User user, Property property);
}
