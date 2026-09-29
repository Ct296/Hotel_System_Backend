package com.hotel.system.repository;

import com.hotel.system.entity.Room;
import com.hotel.system.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomImageRepository extends JpaRepository<RoomImage, String> {

    List<RoomImage> findAllByRoomOrderByCreateDateAsc(Room room);

    Optional<RoomImage> findFirstByRoomAndIsPrimaryTrue(Room room);

    long countByRoom(Room room);

    void deleteByRoom(Room room);
}
