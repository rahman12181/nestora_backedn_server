package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.PropertyMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, Long> {
    List<PropertyMedia> findByPropertyOrderBySortOrderAsc(Property property);
}
