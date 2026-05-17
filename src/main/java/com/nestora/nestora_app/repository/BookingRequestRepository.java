package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.BookingRequest;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByUser(User user);
    List<BookingRequest> findByProperty(Property property);
    List<BookingRequest> findByPropertyIn(List<Property> properties);

}
