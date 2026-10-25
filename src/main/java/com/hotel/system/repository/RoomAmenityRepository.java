package com.hotel.system.repository;

import com.hotel.system.entity.RoomAmenity;
import com.hotel.system.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, RoomAmenity.RoomAmenityId> {
    
    List<RoomAmenity> findByRoomType(RoomType roomType);
}