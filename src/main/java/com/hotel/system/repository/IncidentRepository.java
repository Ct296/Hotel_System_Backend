package com.hotel.system.repository;

import com.hotel.system.entity.Incident;
import com.hotel.system.entity.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {

    List<Incident> findByRentalDetail_Id(String rentalDetailId);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findAllByOrderByOccurTimeDesc();

    long countByRentalDetail_Rental_IdAndStatusNot(String rentalId, IncidentStatus status);
}