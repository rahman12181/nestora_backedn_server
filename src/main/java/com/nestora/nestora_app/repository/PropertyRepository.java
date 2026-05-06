package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByOwner(OwnerProfile owner);

    List<Property> findByIsPublishedTrueAndIsActiveTrue();

    @Query("""
        SELECT p FROM Property p
        WHERE p.isPublished = true
        AND p.isActive = true
        AND (:city IS NULL OR LOWER(p.city) = LOWER(:city))
        AND (:type IS NULL OR p.propertyType = :type)
        AND (:gender IS NULL OR p.genderAllowed = :gender OR p.genderAllowed = 'BOTH')
        AND (:minRent IS NULL OR p.monthlyRentMin >= :minRent)
        AND (:maxRent IS NULL OR p.monthlyRentMax <= :maxRent)
    """)
    List<Property> searchProperties(
            @Param("city") String city,
            @Param("type") PropertyType type,
            @Param("gender") GenderAllowed gender,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent
    );
}
