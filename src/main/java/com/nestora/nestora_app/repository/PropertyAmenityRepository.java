package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.PropertyAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, Long> {
    List<PropertyAmenity> findByProperty(Property property);
    void deleteByProperty(Property property);
}