package com.hotel.system.repository;

import com.hotel.system.entity.ServiceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, String> {

    List<ServiceUsage> findAllByOrderByTimeDesc();

    List<ServiceUsage> findByServiceIdOrderByTimeDesc(String serviceId);

    long countByServiceId(String serviceId);

    // Lấy danh sách dịch vụ đã dùng theo chi tiết phòng (RentalDetail)
    List<ServiceUsage> findByRentalDetail_IdOrderByTimeDesc(String rentalDetailId);
    
    // Nếu muốn lấy TẤT CẢ dịch vụ của một Booking tổng (Rental)
    List<ServiceUsage> findByRentalDetail_Rental_IdOrderByTimeDesc(String rentalId);
}