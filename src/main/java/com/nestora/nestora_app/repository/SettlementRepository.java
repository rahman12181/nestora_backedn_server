package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT s FROM Settlement s WHERE s.owner = :owner " +
            "ORDER BY s.createdAt DESC")
    List<Settlement> findByOwnerOrderByCreatedAtDesc(@Param("owner") OwnerProfile owner);

    Optional<Settlement> findBySettlementCode(String code);

    @Query("SELECT s FROM Settlement s WHERE s.status = 'PENDING'")
    List<Settlement> findPending();
}