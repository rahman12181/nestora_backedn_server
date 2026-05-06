package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Conversation;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserOrOwnerUser(User user, User ownerUser);
    Optional<Conversation> findByUserAndOwnerUserAndPropertyId(
            User user, User ownerUser, Long propertyId
    );
}
