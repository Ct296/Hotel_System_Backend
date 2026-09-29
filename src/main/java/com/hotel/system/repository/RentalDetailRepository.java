package com.hotel.system.repository;

import com.hotel.system.entity.RentalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RentalDetailRepository extends JpaRepository<RentalDetail, String> {
    // Tìm các chi tiết phòng thuộc về 1 đơn đặt (Booking)
    List<RentalDetail> findByRental_Id(String rentalId);
    
    // Tìm các booking detail theo phòng (hỗ trợ kiểm tra lịch trống)
    List<RentalDetail> findByRoom_Id(String roomId);
}