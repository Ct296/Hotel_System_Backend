package com.hotel.system.repository;

import com.hotel.system.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, RoomAmenity.RoomAmenityId> {
    // Lấy danh sách tiện nghi của một loại phòng
    List<RoomAmenity> findByRoomType_Id(String roomTypeId);
}