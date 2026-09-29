package com.hotel.system.repository;

import com.hotel.system.entity.WorkSchedule;
import com.hotel.system.entity.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String> {

    List<WorkSchedule> findAllByOrderByDateDesc();

    boolean existsByDateAndShift(LocalDate date, Shift shift);
}