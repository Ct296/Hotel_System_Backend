package com.hotel.system.service;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerHrService {

    private final WorkScheduleRepository workScheduleRepository;
    private final StaffRepository staffRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final HistoryWorkRepository historyWorkRepository;

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MONTH_PARAM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    // 1. CÁC NGHIỆP VỤ THAO TÁC DỮ LIỆU (POST)

    @Transactional
    public void createWorkSchedule(LocalDate date, Shift shift) {
        ScheduleWindow window = resolveWindow(date, shift);
        if (window.end().plusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể tạo ca trong quá khứ.");
        }
        if (workScheduleRepository.existsByDateAndShift(date, shift)) {
            throw new IllegalArgumentException("Ngày này đã có ca " + shift.getDisplayName() + ".");
        }
        WorkSchedule ws = new WorkSchedule();
        ws.setId(AppUtils.generateId("WKS", 10));
        ws.setDate(date);
        ws.setShift(shift);
        workScheduleRepository.save(ws);
    }

    @Transactional
    public void assignStaff(String scheduleId, String staffId, String note) {
        WorkSchedule schedule = workScheduleRepository.findById(scheduleId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca."));
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());

        if (window.end().plusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể phân công nhân viên vào ca đã qua.");
        }
        if (workAssignmentRepository.existsByStaffIdAndWorkScheduleId(staffId, scheduleId)) {
            throw new IllegalArgumentException("Nhân viên này đã được phân công vào ca từ trước.");
        }

        WorkAssignment assignment = new WorkAssignment();
        assignment.setId(AppUtils.generateId("WAT", 10));
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setNote(note != null && !note.isBlank() ? note.trim() : null);
        assignment.setStaff(staff);
        assignment.setWorkSchedule(schedule);
        workAssignmentRepository.save(assignment);
        ensureHistoryWork(staff, schedule);
    }

    @Transactional
    public void checkin(String scheduleId, String staffId) {
        WorkSchedule schedule = workScheduleRepository.findById(scheduleId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca."));
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));

        if (!workAssignmentRepository.existsByStaffIdAndWorkScheduleId(staffId, scheduleId)) {
            throw new IllegalArgumentException("Nhân viên chưa được phân công vào ca này.");
        }

        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(window.start().minusMinutes(15))) {
            throw new IllegalArgumentException("Chưa tới thời gian check-in.");
        }
        if (now.isAfter(window.end().plusMinutes(15))) {
            HistoryWork missed = ensureHistoryWork(staff, schedule);
            missed.setStatus(AttendanceStatus.MISSED);
            historyWorkRepository.save(missed);
            throw new IllegalStateException("Ca đã kết thúc. Hệ thống đã tự động ghi nhận vắng.");
        }

        HistoryWork hw = ensureHistoryWork(staff, schedule);
        if (hw.getStatus() == AttendanceStatus.COMPLETED) throw new IllegalArgumentException("Nhân viên đã hoàn tất ca này.");
        if (hw.getCheckinTime() != null) throw new IllegalArgumentException("Nhân viên đã check-in từ trước.");

        hw.setCheckinTime(now);
        hw.setStatus(AttendanceStatus.CHECKED_IN);
        historyWorkRepository.save(hw);
    }

    @Transactional
    public void checkout(String scheduleId, String staffId) {
        WorkSchedule schedule = workScheduleRepository.findById(scheduleId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca."));
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(window.end().minusMinutes(15))) {
            throw new IllegalArgumentException("Chưa tới giờ checkout.");
        }
        if (now.isAfter(window.end().plusMinutes(15))) {
            HistoryWork hw = ensureHistoryWork(staff, schedule);
            if (hw.getCheckoutTime() == null) {
                hw.setStatus(AttendanceStatus.MISSED);
                historyWorkRepository.save(hw);
            }
            throw new IllegalStateException("Ca đã qua giờ checkout. Hệ thống đã tự động ghi nhận.");
        }

        HistoryWork hw = ensureHistoryWork(staff, schedule);
        if (hw.getCheckinTime() == null) throw new IllegalArgumentException("Nhân viên chưa check-in.");
        if (hw.getCheckoutTime() != null) throw new IllegalArgumentException("Nhân viên đã checkout.");

        hw.setCheckoutTime(now);
        hw.setStatus(AttendanceStatus.COMPLETED);
        
        // --- LOGIC TỰ ĐỘNG TÍNH TĂNG CA (OVERTIME) ---
        long overtimeMinutes = java.time.temporal.ChronoUnit.MINUTES.between(window.end(), now);
        if (overtimeMinutes > 30) { // Làm lố hơn 30 phút mới tính là tăng ca
            // Làm tròn 1 chữ số thập phân (VD: 1.5 giờ)
            double otHours = Math.round((overtimeMinutes / 60.0) * 10.0) / 10.0;
            hw.setOvertimeHours(otHours);
        }
        
        historyWorkRepository.save(hw);
    }

    @Transactional
    public void updateAttendanceNoteAndOvertime(String scheduleId, String staffId, Double overtimeHours, String note) {
        HistoryWork hw = historyWorkRepository.findByStaffIdAndWorkScheduleId(staffId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi chấm công."));
        
        // Nếu OT < 0 thì lưu null hoặc 0.0 tùy ý, ở đây mình set về null nếu <= 0 cho sạch DB
        hw.setOvertimeHours(overtimeHours != null && overtimeHours > 0 ? overtimeHours : null);
        hw.setNote(note != null && !note.isBlank() ? note.trim() : null);
        
        historyWorkRepository.save(hw);
    }

    @Transactional
    public void removeAssignment(String assignmentId) {
        WorkAssignment assignment = workAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công."));
        WorkSchedule schedule = assignment.getWorkSchedule();

        if (schedule != null) {
            HistoryWork hw = historyWorkRepository.findByStaffIdAndWorkScheduleId(assignment.getStaff().getId(), schedule.getId()).orElse(null);
            if (hw != null && (hw.getCheckinTime() != null || hw.getCheckoutTime() != null)) {
                throw new IllegalStateException("Không thể gỡ phân công khi nhân viên đã bắt đầu chấm công.");
            }
            if (hw != null) historyWorkRepository.delete(hw);
        }
        workAssignmentRepository.delete(assignment);
    }

    @Transactional
    public void deleteWorkSchedule(String scheduleId) {
        WorkSchedule schedule = workScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm."));

        List<HistoryWork> historyWorks = historyWorkRepository.findByWorkScheduleIdOrderByStaffIdAsc(scheduleId);
        if (historyWorks.stream().anyMatch(h -> h.getCheckinTime() != null || h.getCheckoutTime() != null)) {
            throw new IllegalStateException("Không thể xóa ca làm đã phát sinh dữ liệu chấm công thực tế.");
        }

        historyWorkRepository.deleteAll(historyWorks);
        workAssignmentRepository.deleteAll(workAssignmentRepository.findByWorkScheduleIdOrderByAssignedAtDesc(scheduleId));
        workScheduleRepository.delete(schedule);
    }

    // 2. CÁC HÀM XÂY DỰNG DỮ LIỆU VIEW (DASHBOARD)

    @Transactional
    public void syncAttendanceRecords(List<WorkAssignment> assignments) {
        for (WorkAssignment a : assignments) {
            if (a.getStaff() != null && a.getWorkSchedule() != null && a.getEndAt() == null) {
                ensureHistoryWork(a.getStaff(), a.getWorkSchedule());
            }
        }
        List<HistoryWork> allHistoryWorks = historyWorkRepository.findAll();
        boolean dirty = false;
        for (HistoryWork hw : allHistoryWorks) {
            if (hw.getWorkSchedule() == null || hw.getWorkSchedule().getShift() == null || hw.getWorkSchedule().getDate() == null) continue;
            AttendanceStatus nextStatus = determineStatus(hw.getWorkSchedule(), hw.getCheckinTime(), hw.getCheckoutTime());
            if (hw.getStatus() != nextStatus) {
                hw.setStatus(nextStatus);
                dirty = true;
            }
        }
        if (dirty) historyWorkRepository.saveAll(allHistoryWorks);
    }

    public Map<LocalDate, List<ScheduleSummaryView>> buildScheduleSummaries(List<WorkSchedule> schedules, List<WorkAssignment> assignments) {
        Map<String, List<WorkAssignment>> assignmentsByScheduleId = assignments.stream()
                .filter(a -> a.getWorkSchedule() != null && a.getWorkSchedule().getId() != null)
                .collect(Collectors.groupingBy(a -> a.getWorkSchedule().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<LocalDate, List<ScheduleSummaryView>> result = new LinkedHashMap<>();
        for (WorkSchedule schedule : schedules) {
            List<StaffMiniView> staffMiniViews = assignmentsByScheduleId.getOrDefault(schedule.getId(), List.of()).stream()
                    .filter(a -> a.getEndAt() == null && a.getStaff() != null)
                    .map(a -> {
                        // LOGIC LẤY TRẠNG THÁI CHẤM CÔNG GẮN VÀO DASHBOARD
                        HistoryWork hw = historyWorkRepository.findByStaffIdAndWorkScheduleId(a.getStaff().getId(), schedule.getId()).orElse(null);
                        AttendanceStatus status = hw != null ? hw.getStatus() : AttendanceStatus.NOT_ATTENDED;
                        String badgeClass = switch (status) {
                            case CHECKED_IN -> "badge bg-success text-white";
                            case COMPLETED -> "badge bg-primary text-white";
                            case MISSED -> "badge bg-danger text-white";
                            default -> "badge bg-secondary text-white";
                        };
                        // Đã truyền đủ 7 tham số
                        return new StaffMiniView(a.getId(), a.getStaff().getId(), shortNameOf(a.getStaff()), fullNameOf(a.getStaff()), a.getNote(), status.getDisplayName(), badgeClass);
                    })
                    .toList();

            ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
            String badgeClass = staffMiniViews.isEmpty() ? "empty" : "filled";

            result.computeIfAbsent(schedule.getDate(), k -> new ArrayList<>())
                    .add(new ScheduleSummaryView(schedule.getId(), schedule.getShift(), schedule.getShift().getDisplayName(),
                            window.start().toLocalTime().toString(), formatRange(window.start(), window.end()),
                            staffMiniViews, badgeClass, schedule.getDate().equals(LocalDate.now())));
        }
        result.values().forEach(list -> list.sort(Comparator.comparingInt(v -> v.shift().ordinal())));
        return result;
    }

    public List<List<CalendarDayView>> buildCalendarWeeks(YearMonth yearMonth, Map<LocalDate, List<ScheduleSummaryView>> summariesByDate, LocalDate selectedDate) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        LocalDate cursor = firstDay.with(DayOfWeek.MONDAY);
        LocalDate calendarEnd = lastDay.with(DayOfWeek.SUNDAY);

        List<List<CalendarDayView>> weeks = new ArrayList<>();
        while (!cursor.isAfter(calendarEnd)) {
            List<CalendarDayView> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                List<ScheduleSummaryView> schedules = summariesByDate.getOrDefault(cursor, List.of());
                week.add(new CalendarDayView(cursor.toString(), String.valueOf(cursor.getDayOfMonth()),
                        cursor.equals(LocalDate.now()), cursor.equals(selectedDate),
                        cursor.getMonth().equals(yearMonth.getMonth()), !schedules.isEmpty(), schedules,
                        capitalize(cursor.getDayOfWeek().getDisplayName(TextStyle.SHORT, VI_LOCALE))));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    public List<AttendanceScheduleView> buildAttendanceSchedules(List<WorkSchedule> schedules, List<WorkAssignment> assignments, List<HistoryWork> historyWorks) {
        Map<String, List<WorkAssignment>> assignmentsByScheduleId = assignments.stream()
                .filter(a -> a.getWorkSchedule() != null && a.getWorkSchedule().getId() != null)
                .collect(Collectors.groupingBy(a -> a.getWorkSchedule().getId()));

        Map<String, List<HistoryWork>> historyByScheduleId = historyWorks.stream()
                .filter(h -> h.getWorkSchedule() != null && h.getWorkSchedule().getId() != null)
                .collect(Collectors.groupingBy(h -> h.getWorkSchedule().getId()));

        return schedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getDate).reversed().thenComparing(ws -> ws.getShift().ordinal()))
                .map(schedule -> {
                    ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
                    List<WorkAssignment> scheduleAssignments = assignmentsByScheduleId.getOrDefault(schedule.getId(), List.of()).stream()
                            .filter(a -> a.getEndAt() == null)
                            .sorted(Comparator.comparing(a -> fullNameOf(a.getStaff()))).toList();

                    Map<String, HistoryWork> historyByStaffId = historyByScheduleId.getOrDefault(schedule.getId(), List.of()).stream()
                            .filter(h -> h.getStaff() != null && h.getStaff().getId() != null)
                            .collect(Collectors.toMap(h -> h.getStaff().getId(), h -> h, (l, r) -> l, LinkedHashMap::new));

                    List<AttendanceRowView> rows = scheduleAssignments.stream().map(a -> {
                        Staff staff = a.getStaff();
                        HistoryWork history = staff == null ? null : historyByStaffId.get(staff.getId());
                        AttendanceStatus status = history != null ? history.getStatus() : determineStatus(schedule, null, null);
                        return new AttendanceRowView(
                            staff != null ? staff.getId() : "", fullNameOf(staff),
                            staff != null && staff.getUser() != null && staff.getUser().getEmail() != null ? staff.getUser().getEmail() : "-",
                            status, status.getDisplayName(),
                            formatDateTime(history != null ? history.getCheckinTime() : null),
                            formatDateTime(history != null ? history.getCheckoutTime() : null),
                            canCheckin(schedule, history), canCheckout(schedule, history),
                            history != null ? history.getOvertimeHours() : null,
                            history != null ? history.getNote() : null
                        );
                    }).toList();

                    long completed = rows.stream().filter(r -> r.status() == AttendanceStatus.COMPLETED).count();
                    long missed = rows.stream().filter(r -> r.status() == AttendanceStatus.MISSED).count();

                    return new AttendanceScheduleView(
                            schedule.getId(), schedule.getDate().format(DATE_FORMAT), schedule.getShift().getDisplayName(),
                            formatRange(window.start(), window.end()), rows, completed, missed,
                            schedule.getDate().isBefore(LocalDate.now()) || LocalDateTime.now().isAfter(window.end().plusMinutes(15))
                    );
                }).toList();
    }

    public Map<String, Long> buildStatusCountMap(List<HistoryWork> historyWorks) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            result.put(status.getDisplayName(), historyWorks.stream().filter(h -> h.getStatus() == status).count());
        }
        return result;
    }

    // 3. PRIVATE HELPER METHODS

    private HistoryWork ensureHistoryWork(Staff staff, WorkSchedule schedule) {
        return historyWorkRepository.findByStaffIdAndWorkScheduleId(staff.getId(), schedule.getId())
                .map(hw -> {
                    AttendanceStatus computed = determineStatus(schedule, hw.getCheckinTime(), hw.getCheckoutTime());
                    if (hw.getStatus() != computed) {
                        hw.setStatus(computed);
                        historyWorkRepository.save(hw);
                    }
                    return hw;
                }).orElseGet(() -> {
                    HistoryWork hw = new HistoryWork();
                    hw.setId(AppUtils.generateId("HWK", 10));
                    hw.setStatus(determineStatus(schedule, null, null));
                    hw.setStaff(staff);
                    hw.setWorkSchedule(schedule);
                    return historyWorkRepository.save(hw);
                });
    }

    private AttendanceStatus determineStatus(WorkSchedule schedule, LocalDateTime checkin, LocalDateTime checkout) {
        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
        LocalDateTime now = LocalDateTime.now();
        if (checkin != null && checkout != null) return AttendanceStatus.COMPLETED;
        if (checkin != null) return now.isAfter(window.end().plusMinutes(15)) ? AttendanceStatus.MISSED : AttendanceStatus.CHECKED_IN;
        if (now.isBefore(window.start().minusMinutes(15))) return AttendanceStatus.NOT_STARTED;
        if (now.isAfter(window.end().plusMinutes(15))) return AttendanceStatus.MISSED;
        return AttendanceStatus.NOT_ATTENDED;
    }

    private ScheduleWindow resolveWindow(LocalDate date, Shift shift) {
        return switch (shift) {
            case NIGHT -> new ScheduleWindow(date.atStartOfDay(), date.atTime(6, 0));
            case MORNING -> new ScheduleWindow(date.atTime(6, 0), date.atTime(12, 0));
            case AFTERNOON -> new ScheduleWindow(date.atTime(12, 0), date.atTime(18, 0));
            case EVENING -> new ScheduleWindow(date.atTime(18, 0), date.plusDays(1).atStartOfDay());
        };
    }

    private boolean canCheckin(WorkSchedule schedule, HistoryWork history) {
        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(window.start().minusMinutes(15)) || now.isAfter(window.end().plusMinutes(15))) return false;
        return history == null || history.getCheckinTime() == null;
    }

    private boolean canCheckout(WorkSchedule schedule, HistoryWork history) {
        if (history == null || history.getCheckinTime() == null || history.getCheckoutTime() != null) return false;
        ScheduleWindow window = resolveWindow(schedule.getDate(), schedule.getShift());
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(window.end().minusMinutes(15)) && !now.isAfter(window.end().plusMinutes(15));
    }

    public YearMonth parseMonth(String month) {
        try { return month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month, MONTH_PARAM_FORMAT); } 
        catch (Exception ex) { return YearMonth.now(); }
    }

    public LocalDate parseSelectedDate(String selectedDate, YearMonth fallbackMonth) {
        try { 
            if (selectedDate != null && !selectedDate.isBlank()) return LocalDate.parse(selectedDate); 
        } catch (Exception ignored) {}
        
        // NẾU LÀ THÁNG HIỆN TẠI -> LẤY NGÀY HÔM NAY (Thời gian thực). KHÁC THÁNG -> LẤY NGÀY 1
        if (fallbackMonth.equals(YearMonth.now())) {
            return LocalDate.now();
        }
        return fallbackMonth.atDay(1);
    }

    public String formatMonthTitle(YearMonth ym) {
        return capitalize(ym.getMonth().getDisplayName(TextStyle.FULL, VI_LOCALE)) + " " + ym.getYear();
    }

    private String formatDateTime(LocalDateTime value) { return value == null ? "-" : value.format(DATETIME_FORMAT); }
    private String formatRange(LocalDateTime start, LocalDateTime end) { return start.toLocalTime().toString() + " - " + end.toLocalTime().toString(); }
    
    private String fullNameOf(Staff staff) {
        if (staff == null || staff.getUser() == null) return "N/A";
        String fullName = (AppUtils.safeTrim(staff.getUser().getLastName()) + " " + AppUtils.safeTrim(staff.getUser().getFirstName())).trim();
        return fullName.isBlank() ? "N/A" : fullName;
    }
    
    private String shortNameOf(Staff staff) {
        String full = fullNameOf(staff);
        if ("N/A".equals(full)) return full;
        String[] parts = full.split("\\s+");
        return parts.length == 1 ? parts[0] : parts[parts.length - 1];
    }
    
    private String capitalize(String value) { return value == null || value.isBlank() ? "" : value.substring(0, 1).toUpperCase(VI_LOCALE) + value.substring(1); }

    // 4. VIEW RECORDS (DTOs for Thymeleaf)
    private record ScheduleWindow(LocalDateTime start, LocalDateTime end) {}
    public record StaffMiniView(String assignmentId, String staffId, String shortName, String fullName, String note, String statusText, String statusClass) {}
    public record ScheduleSummaryView(String scheduleId, Shift shift, String shiftName, String startTimeText, String rangeText, List<StaffMiniView> staffs, String badgeClass, boolean today) {}
    public record CalendarDayView(String value, String dayNumber, boolean today, boolean selected, boolean inCurrentMonth, boolean hasSchedule, List<ScheduleSummaryView> schedules, String dayName) {}
    public record AttendanceRowView(String staffId, String staffName, String email, AttendanceStatus status, String statusText, String checkinText, String checkoutText, boolean canCheckin, boolean canCheckout, Double overtimeHours, String note) {}
    public record AttendanceScheduleView(String scheduleId, String dateText, String shiftName, String rangeText, List<AttendanceRowView> rows, long completedCount, long missedCount, boolean inPast) {}
    
    
}