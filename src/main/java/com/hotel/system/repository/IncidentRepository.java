package com.hotel.system.repository;

import com.hotel.system.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    // Lấy các sự cố của một chi tiết thuê phòng
    List<Incident> findByRentalDetail_Id(String rentalDetailId);
}