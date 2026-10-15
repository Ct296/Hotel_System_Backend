package com.hotel.system.repository;

import com.hotel.system.entity.Rental;
import com.hotel.system.entity.enums.RentalDetailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, String> {
    List<Rental> findByCustomerId(String userId);

    List<Rental> findByRentalDetails_RoomIdAndRentalDetails_StatusIn(String roomId, Collection<RentalDetailStatus> statuses);
}