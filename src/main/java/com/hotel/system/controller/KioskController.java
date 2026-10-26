package com.hotel.system.controller;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.Shift;
import com.hotel.system.repository.*;
import com.hotel.system.service.ManagerHrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final HistoryWorkRepository historyWorkRepository;
    private final ManagerHrService managerHrService;
    private final StaffRepository staffRepository;

    @PostMapping("/swipe")
    public ResponseEntity<?> swipeCard(@RequestBody Map<String, String> payload) {
        String staffId = payload.get("staffId");
        if (staffId == null || staffId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập mã nhân viên."));
        }

        Staff staff = staffRepository.findById(staffId.trim().toUpperCase()).orElse(null);
        if (staff == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy hồ sơ nhân viên."));
        }

        // Tìm ca làm việc trong ngày hôm nay (hoặc hôm qua nếu là ca Đêm vắt sang ngày mới)
        LocalDate today = LocalDate.now();
        List<WorkAssignment> assignments = workAssignmentRepository.findByStaffIdOrderByAssignedAtDesc(staff.getId());
        
        WorkAssignment currentAssignment = null;
        LocalDateTime now = LocalDateTime.now();

        for (WorkAssignment a : assignments) {
            if (a.getEndAt() != null) continue; // Đã bị gỡ phân công
            WorkSchedule ws = a.getWorkSchedule();
            if (ws != null && (ws.getDate().equals(today) || ws.getDate().equals(today.minusDays(1)))) {
                LocalDateTime start = resolveStart(ws.getDate(), ws.getShift());
                LocalDateTime end = resolveEnd(ws.getDate(), ws.getShift());
                
                // Cửa sổ chấm công: Mở trước 30 phút, đóng sau 60 phút khi hết ca
                if (now.isAfter(start.minusMinutes(30)) && now.isBefore(end.plusMinutes(60))) {
                    currentAssignment = a;
                    break;
                }
            }
        }

        if (currentAssignment == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Xin chào " + staff.getUser().getFirstName() + ", bạn không có ca làm việc vào lúc này!"));
        }

        String scheduleId = currentAssignment.getWorkSchedule().getId();
        Optional<HistoryWork> hwOpt = historyWorkRepository.findByStaffIdAndWorkScheduleId(staff.getId(), scheduleId);

        try {
            if (hwOpt.isPresent() && hwOpt.get().getCheckinTime() != null && hwOpt.get().getCheckoutTime() == null) {
                // Đã checkin -> Tiến hành checkout
                managerHrService.checkout(scheduleId, staff.getId());
                return ResponseEntity.ok(Map.of("message", "Chào tạm biệt " + staff.getUser().getFirstName() + ", Check-out thành công!", "action", "OUT"));
            } else {
                // Chưa checkin -> Tiến hành checkin
                managerHrService.checkin(scheduleId, staff.getId());
                return ResponseEntity.ok(Map.of("message", "Xin chào " + staff.getUser().getFirstName() + ", Check-in thành công!", "action", "IN"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private LocalDateTime resolveStart(LocalDate date, Shift shift) {
        return switch (shift) {
            case NIGHT -> date.atStartOfDay();
            case MORNING -> date.atTime(6, 0);
            case AFTERNOON -> date.atTime(12, 0);
            case EVENING -> date.atTime(18, 0);
        };
    }

    private LocalDateTime resolveEnd(LocalDate date, Shift shift) {
        return switch (shift) {
            case NIGHT -> date.atTime(6, 0);
            case MORNING -> date.atTime(12, 0);
            case AFTERNOON -> date.atTime(18, 0);
            case EVENING -> date.plusDays(1).atStartOfDay();
        };
    }
}