package com.hotel.system.repository;

import com.hotel.system.entity.RentalGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalGuestRepository extends JpaRepository<RentalGuest, String> {
    List<RentalGuest> findByRentalDetailId(String rentalDetailId);
    void deleteByRentalDetailId(String rentalDetailId);
}