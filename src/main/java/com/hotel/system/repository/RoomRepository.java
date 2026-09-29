package com.hotel.system.repository;

import com.hotel.system.entity.Room;
import com.hotel.system.entity.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findByRoomTypeId(String typeId);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}