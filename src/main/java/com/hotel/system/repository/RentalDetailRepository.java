package com.hotel.system.repository;

import com.hotel.system.entity.RentalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalDetailRepository extends JpaRepository<RentalDetail, String> {
}