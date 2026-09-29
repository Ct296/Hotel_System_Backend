package com.hotel.system.repository;

import com.hotel.system.entity.WorkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, String> {

    List<WorkAssignment> findByStaffIdOrderByAssignedAtDesc(String staffId);

    List<WorkAssignment> findByWorkScheduleIdOrderByAssignedAtDesc(String workScheduleId);

    List<WorkAssignment> findByStaffIdAndEndAtIsNullOrderByAssignedAtDesc(String staffId);

    List<WorkAssignment> findByWorkScheduleIdAndEndAtIsNullOrderByAssignedAtDesc(String workScheduleId);

    Optional<WorkAssignment> findByStaffIdAndWorkScheduleId(String staffId, String workScheduleId);

    boolean existsByStaffIdAndWorkScheduleId(String staffId, String workScheduleId);

    long countByWorkScheduleId(String workScheduleId);

    List<WorkAssignment> findByStaffIdAndAssignedAtBetweenOrderByAssignedAtDesc(
            String staffId,
            LocalDateTime start,
            LocalDateTime end
    );
}