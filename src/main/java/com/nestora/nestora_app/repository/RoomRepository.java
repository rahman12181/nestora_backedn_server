package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.Room;
import com.nestora.nestora_app.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByProperty(Property property);
    List<Room> findByPropertyAndStatus(Property property, RoomStatus status);
    int countByPropertyAndStatus(Property property, RoomStatus status);
}
