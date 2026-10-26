package com.hotel.system.controller;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.AttendanceStatus;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.entity.enums.Shift;
import com.hotel.system.entity.enums.WorkingStatus;
import com.hotel.system.repository.*;
import com.hotel.system.service.AdminAccountService;
import com.hotel.system.service.ManagerHrService;
import com.hotel.system.service.ManagerHrService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/manager/hr")
@RequiredArgsConstructor
public class ManagerHrController {

    private final StaffRepository staffRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final HistoryWorkRepository historyWorkRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final UsersRepository usersRepository;
    private final ManagerRepository managerRepository;
    private final ManagerHrService managerHrService;
    private final AdminAccountService adminAccountService;
    private final AccountRepository accountRepository;
    private final AccountStatusRepository accountStatusRepository;

    private boolean hasHrAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) return true;
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            Users user = usersRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() == Role.MANAGER) {
                Manager manager = managerRepository.findById(user.getId()).orElse(null);
                return manager != null && manager.getTitle().name().equals("HR_MANAGER");
            }
        }
        return false;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestParam(required = false) String month,
                                       @RequestParam(required = false) String selectedDate,
                                       @RequestParam(required = false) String attendanceScheduleId) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));

        List<Staff> staffs = staffRepository.findAllByOrderByEmploymentTimeDesc();
        List<WorkSchedule> allSchedules = workScheduleRepository.findAll().stream().sorted(Comparator.comparing(WorkSchedule::getDate).thenComparing(ws -> ws.getShift().ordinal())).toList();
        List<WorkAssignment> allAssignments = workAssignmentRepository.findAll().stream().sorted(Comparator.comparing(WorkAssignment::getAssignedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        List<HistoryWork> allHistoryWorks = historyWorkRepository.findAll().stream().sorted(Comparator.comparing((HistoryWork hw) -> hw.getWorkSchedule().getDate()).reversed().thenComparing(hw -> hw.getWorkSchedule().getShift().ordinal())).toList();

        managerHrService.syncAttendanceRecords(allAssignments);
        YearMonth currentMonth = managerHrService.parseMonth(month);
        LocalDate currentSelectedDate = managerHrService.parseSelectedDate(selectedDate, currentMonth);

        Map<LocalDate, List<ScheduleSummaryView>> scheduleSummariesByDate = managerHrService.buildScheduleSummaries(allSchedules, allAssignments);
        List<List<CalendarDayView>> calendarWeeks = managerHrService.buildCalendarWeeks(currentMonth, scheduleSummariesByDate, currentSelectedDate);
        List<ScheduleSummaryView> selectedDateSchedules = scheduleSummariesByDate.getOrDefault(currentSelectedDate, List.of());
        
        List<AttendanceScheduleView> attendanceSchedules = managerHrService.buildAttendanceSchedules(allSchedules, allAssignments, allHistoryWorks);
        
        String resolvedAttendanceScheduleId = (attendanceScheduleId == null || attendanceScheduleId.isBlank()) && !selectedDateSchedules.isEmpty() 
                ? selectedDateSchedules.get(0).scheduleId() : attendanceScheduleId;
        
        AttendanceScheduleView selectedAttendanceSchedule = attendanceSchedules.stream()
                .filter(item -> resolvedAttendanceScheduleId != null && item.scheduleId().equals(resolvedAttendanceScheduleId))
                .findFirst().orElse(attendanceSchedules.isEmpty() ? null : attendanceSchedules.get(0));

        Map<String, Object> response = new HashMap<>();
        response.put("staffs", staffs);
        response.put("shifts", Shift.values());
        response.put("monthValue", currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        response.put("monthTitle", managerHrService.formatMonthTitle(currentMonth));
        response.put("calendarWeeks", calendarWeeks);
        response.put("selectedDateValue", currentSelectedDate.toString());
        response.put("selectedDateLabel", currentSelectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        response.put("selectedDateSchedules", selectedDateSchedules);
        response.put("attendanceSchedules", attendanceSchedules);
        response.put("selectedAttendanceSchedule", selectedAttendanceSchedule);
        
        response.put("staffCount", staffs.size());
        response.put("scheduleCount", allSchedules.size());
        response.put("activeAssignmentCount", allAssignments.stream().filter(item -> item.getEndAt() == null).count());
        response.put("completedAttendanceCount", allHistoryWorks.stream().filter(item -> item.getStatus() == AttendanceStatus.COMPLETED).count());
        response.put("missedAttendanceCount", allHistoryWorks.stream().filter(item -> item.getStatus() == AttendanceStatus.MISSED).count());
        response.put("attendanceStatusCount", managerHrService.buildStatusCountMap(allHistoryWorks));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/work-schedules")
    public ResponseEntity<?> saveWorkSchedule(@RequestBody Map<String, String> payload) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            LocalDate scheduleDate = LocalDate.parse(payload.get("date"));
            Shift shift = Shift.valueOf(payload.get("shift"));
            managerHrService.createWorkSchedule(scheduleDate, shift);
            return ResponseEntity.ok(Map.of("message", "Tạo ca " + shift.getDisplayName() + " thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/work-assignments")
    public ResponseEntity<?> saveWorkAssignment(@RequestBody Map<String, String> payload) {
        return assignStaffToWorkSchedule(payload.get("workScheduleId"), payload);
    }

    @PostMapping("/work-schedules/{workScheduleId}/assign-staff")
    public ResponseEntity<?> assignStaffToWorkSchedule(@PathVariable String workScheduleId, @RequestBody Map<String, String> payload) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            managerHrService.assignStaff(workScheduleId, payload.get("staffId"), payload.get("note"));
            return ResponseEntity.ok(Map.of("message", "Phân công nhân viên thành công."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/work-schedules/{workScheduleId}/checkin/{staffId}")
    public ResponseEntity<?> checkin(@PathVariable String workScheduleId, @PathVariable String staffId) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            managerHrService.checkin(workScheduleId, staffId);
            return ResponseEntity.ok(Map.of("message", "Check-in thành công."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/work-schedules/{workScheduleId}/checkout/{staffId}")
    public ResponseEntity<?> checkout(@PathVariable String workScheduleId, @PathVariable String staffId) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            managerHrService.checkout(workScheduleId, staffId);
            return ResponseEntity.ok(Map.of("message", "Check-out thành công."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/work-schedules/{workScheduleId}/attendance/{staffId}")
    public ResponseEntity<?> updateAttendanceRecord(@PathVariable String workScheduleId, @PathVariable String staffId, @RequestBody Map<String, Object> payload) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            Double overtimeHours = payload.get("overtimeHours") != null ? Double.valueOf(payload.get("overtimeHours").toString()) : null;
            String note = (String) payload.get("note");
            managerHrService.updateAttendanceNoteAndOvertime(workScheduleId, staffId, overtimeHours, note);
            return ResponseEntity.ok(Map.of("message", "Cập nhật ghi chú và giờ làm thêm thành công."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/work-assignments/{id}")
    public ResponseEntity<?> removeAssignment(@PathVariable String id) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            managerHrService.removeAssignment(id);
            return ResponseEntity.ok(Map.of("message", "Đã gỡ phân công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/work-schedules/{id}")
    public ResponseEntity<?> deleteWorkSchedule(@PathVariable String id) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            managerHrService.deleteWorkSchedule(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa ca làm việc."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/staffs/{staffId}/status")
    public ResponseEntity<?> updateStaffStatus(@PathVariable String staffId, @RequestBody Map<String, String> payload) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Nhân viên với ID: " + staffId));

            WorkingStatus newStatus = WorkingStatus.valueOf(payload.get("workingStatus"));
            staff.setWorkingStatus(newStatus);
            staffRepository.save(staff);

            if (newStatus == WorkingStatus.RESIGNED || newStatus == WorkingStatus.SUSPENDED) {
                String reason = (newStatus == WorkingStatus.RESIGNED) ? "Nhân viên đã nghỉ việc." : "Nhân viên đang bị đình chỉ/nghỉ tạm thời.";
                adminAccountService.lockAccount(staff.getUser().getId(), -1, "Hệ thống tự động khóa: " + reason);
            }

            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái nhân viên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/staffs")
    public ResponseEntity<?> getAllStaffs() {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        // Chỉ lấy user có Role là STAFF
        List<Users> staffs = usersRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STAFF)
                .toList();

        List<Map<String, Object>> response = new ArrayList<>();
        for (Users u : staffs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getLastName() + " " + u.getFirstName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhoneNumber());
            map.put("gender", u.getSex() != null ? u.getSex().name() : "UNSPECIFIED");
            
            Staff staffInfo = staffRepository.findById(u.getId()).orElse(null);
            if (staffInfo != null) {
                map.put("workingStatus", staffInfo.getWorkingStatus().name());
                map.put("employmentTime", staffInfo.getEmploymentTime());
            } else {
                map.put("workingStatus", "UNKNOWN");
            }

            // Lấy trạng thái khóa/mở của tài khoản (Sử dụng findByUser_Id)
            List<AccountStatus> statuses = accountStatusRepository.findByAccount_User_IdOrderByStartTimeDesc(u.getId());
            map.put("accountStatus", statuses.isEmpty() ? "UNKNOWN" : statuses.get(0).getName().toString());
            
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/staffs")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> payload) {
        if (!hasHrAccess()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String email = payload.get("email");
        if (usersRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại trong hệ thống."));
        }

        // 1. Tạo USER (Mặc định Role STAFF)
        Users newUser = new Users();
        String newUserId = com.hotel.system.util.AppUtils.generateId("USR", 10);
        newUser.setId(newUserId);
        newUser.setFirstName(payload.get("firstName"));
        newUser.setLastName(payload.get("lastName"));
        newUser.setEmail(email);
        newUser.setPhoneNumber(payload.get("phone"));
        newUser.setRole(Role.STAFF); 
        newUser.setCreateDate(LocalDateTime.now());
        newUser.setUpdateDate(LocalDateTime.now());
        if (payload.containsKey("gender")) newUser.setSex(com.hotel.system.entity.enums.Gender.valueOf(payload.get("gender")));
        if (payload.containsKey("dob") && !payload.get("dob").isBlank()) newUser.setDateOfBirth(LocalDate.parse(payload.get("dob")));
        usersRepository.save(newUser);

        // 2. Tạo STAFF
        Staff staff = new Staff();
        staff.setId(newUserId);
        staff.setEmploymentTime(LocalDateTime.now());
        staff.setWorkingStatus(WorkingStatus.WORKING);
        staffRepository.save(staff);

        // 3. Tạo ACCOUNT (Truyền Object Users vào thay vì ID chuỗi)
        Account account = new Account();
        account.setUser(newUser); // Liên kết với Users
        account.setPassword(payload.get("password"));
        // Lưu xong thì gán ngược lại biến account để bên dưới xài
        account = accountRepository.save(account); 

        // 4. Kích hoạt trạng thái ACTIVE
        AccountStatus accStatus = new AccountStatus();
        accStatus.setId(com.hotel.system.util.AppUtils.generateId("AST", 10));
        
        // CHỖ NÀY ĐÃ SỬA: Truyền đúng biến 'account' vào vì nó @ManyToOne với Account
        accStatus.setAccount(account); 
        
        // CHỖ NÀY ĐÃ SỬA: Dùng đúng tên Enum AccountState của bạn
        accStatus.setName(com.hotel.system.entity.enums.AccountState.ACTIVE); 
        
        accStatus.setStartTime(LocalDateTime.now());
        accStatus.setReason("Tạo tài khoản nhân viên mới");
        accountStatusRepository.save(accStatus);

        return ResponseEntity.ok(Map.of("message", "Thêm nhân viên thành công!"));
    }
}