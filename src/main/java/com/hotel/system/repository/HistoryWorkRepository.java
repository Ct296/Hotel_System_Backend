package com.hotel.system.repository;

import com.hotel.system.entity.HistoryWork;
import com.hotel.system.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryWorkRepository extends JpaRepository<HistoryWork, String> {

    List<HistoryWork> findAllByOrderByCheckinTimeDesc();

    List<HistoryWork> findByStaffIdOrderByCheckinTimeDesc(String staffId);

    List<HistoryWork> findByWorkScheduleIdOrderByStaffIdAsc(String workScheduleId);

    long countByWorkScheduleId(String workScheduleId);

    long countByStatus(AttendanceStatus status);

    Optional<HistoryWork> findByStaffIdAndWorkScheduleId(String staffId, String workScheduleId);

    boolean existsByStaffIdAndWorkScheduleId(String staffId, String workScheduleId);
}
