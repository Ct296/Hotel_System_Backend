package com.hotel.system.repository;

import com.hotel.system.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    // Lấy sự cố theo chi tiết thuê phòng
    List<Incident> findByRentalDetail_Id(String rentalDetailId);
    
    // Lấy sự cố theo trạng thái (dùng cho Manager lọc danh sách chờ duyệt)
    List<Incident> findByStatus(com.hotel.system.entity.enums.IncidentStatus status);
}