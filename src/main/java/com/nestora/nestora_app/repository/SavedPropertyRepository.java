package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.SavedProperty;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {
    List<SavedProperty> findByUser(User user);
    Optional<SavedProperty> findByUserAndProperty(User user, Property property);
    boolean existsByUserAndProperty(User user, Property property);
}
