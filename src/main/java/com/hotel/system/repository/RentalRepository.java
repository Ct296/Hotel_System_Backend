package com.hotel.system.repository;

import com.hotel.system.entity.Rental;
import com.hotel.system.entity.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, String> {
    List<Rental> findByCustomerId(String userId);

    @Query("SELECT DISTINCT r FROM Rental r JOIN r.details d WHERE d.room.id = :roomId AND r.status IN :statuses")
    List<Rental> findRentalsByRoomIdAndStatusIn(@Param("roomId") String roomId, @Param("statuses") Collection<RentalStatus> statuses);
}