// UserSupportTicketRepository.java
package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserSupportTicketRepository extends JpaRepository<UserSupportTicket, Long> {
    List<UserSupportTicket> findByUserOrderByCreatedAtDesc(User user);
    List<UserSupportTicket> findByStatusOrderByCreatedAtDesc(String status);
}