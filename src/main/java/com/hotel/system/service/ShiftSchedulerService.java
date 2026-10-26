package com.hotel.system.service;

import com.hotel.system.entity.enums.Shift;
import com.hotel.system.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftSchedulerService {

    private final WorkScheduleRepository workScheduleRepository;
    private final ManagerHrService managerHrService; // Tận dụng lại hàm tạo ca bạn đã viết sẵn

    /**
     * Tự động chạy vào lúc 00:00 ngày 25 hàng tháng.
     * Cú pháp Cron: Giây Phút Giờ Ngày Tháng Thứ
     */
    @Scheduled(cron = "0 0 0 25 * ?")
    public void generateShiftsForNextMonth() {
        log.info("Bắt đầu tự động khởi tạo 4 ca làm việc cho tháng tiếp theo...");

        // 1. Xác định tháng tiếp theo
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        int daysInMonth = nextMonth.lengthOfMonth();
        int createdCount = 0;

        // 2. Duyệt qua từng ngày của tháng sau
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = nextMonth.atDay(day);

            // 3. Duyệt qua 4 ca (Khuya, Sáng, Chiều, Tối)
            for (Shift shift : Shift.values()) {
                // 4. Nếu chưa có ca này trong DB thì mới tạo
                if (!workScheduleRepository.existsByDateAndShift(date, shift)) {
                    try {
                        managerHrService.createWorkSchedule(date, shift);
                        createdCount++;
                    } catch (Exception e) {
                        log.error("Lỗi khi tạo tự động ca {} ngày {}: {}", shift, date, e.getMessage());
                    }
                }
            }
        }

        log.info("Hoàn tất! Đã tự động tạo {} ca làm việc mới cho tháng {}.", createdCount, nextMonth);
    }
}