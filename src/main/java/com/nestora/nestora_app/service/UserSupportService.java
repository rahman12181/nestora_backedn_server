// UserSupportService.java
package com.nestora.nestora_app.service;

import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.UserSupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSupportService {

    private final UserSupportTicketRepository repo;

    public UserSupportTicket create(User user, String category,
                                    String subject, String description,
                                    Long refId, String refType) {
        String code = "TKT-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        UserSupportTicket ticket = UserSupportTicket.builder()
                .ticketCode(code)
                .user(user)
                .category(category)
                .subject(subject)
                .description(description)
                .referenceId(refId)
                .referenceType(refType)
                .build();

        return repo.save(ticket);
    }

    public List<UserSupportTicket> getMyTickets(User user) {
        return repo.findByUserOrderByCreatedAtDesc(user);
    }
}