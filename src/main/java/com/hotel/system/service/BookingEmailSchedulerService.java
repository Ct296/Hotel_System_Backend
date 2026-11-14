package com.hotel.system.service;

import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RentalStatus;
import com.hotel.system.repository.RentalRepository;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEmailSchedulerService {

    private final RentalRepository rentalRepository;
    private final RentalOperationService rentalOperationService;
    private final BillingService billingService;
    private final EmailService emailService;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    // Bộ nhớ đánh dấu các đơn đã gửi mail nhắc để không spam trùng lặp mỗi phút
    private final Set<String> sentPendingDepositReminderIds = ConcurrentHashMap.newKeySet();
    private final Set<String> sentUpcomingCheckinReminderIds = ConcurrentHashMap.newKeySet();
    private final Set<String> sentOverdueCheckinWarningIds = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void scanAndSendAutomatedEmails() {
        // 1. Quét và hủy các đơn PENDING quá 15 phút (hàm này đã tích hợp gửi mail báo hủy bên trong)
        rentalOperationService.expirePendingBookingsWithoutDeposit();
        rentalOperationService.markCheckedInRentalsAsOverdue();
        rentalOperationService.autoReleaseCleaningRooms();

        LocalDateTime now = LocalDateTime.now();
        List<Rental> allRentals = rentalRepository.findAll();

        for (Rental rental : allRentals) {
            if (!Boolean.TRUE.equals(rental.getIsBooking()) || rental.getCustomer() == null || rental.getCustomer().getUser() == null) {
                continue;
            }

            Users user = rental.getCustomer().getUser();
            String email = user.getEmail();
            if (email == null || email.isBlank()) continue;

            String fullName = AppUtils.buildFullName(user.getFirstName(), user.getLastName());
            String roomNames = extractRoomNames(rental);
            LocalDateTime checkinTime = extractEarliestCheckin(rental);

            // 2. ĐƠN ĐANG CHỜ CỌC (PENDING): Nhắc nhở ở phút thứ 10 (còn 5 phút cuối)
            if (rental.getStatus() == RentalStatus.PENDING && rental.getRentDate() != null) {
                long minutesSinceBooked = ChronoUnit.MINUTES.between(rental.getRentDate(), now);
                if (minutesSinceBooked >= 10 && minutesSinceBooked < 15
                        && billingService.calculateDepositPaid(rental.getId()) <= 0
                        && !sentPendingDepositReminderIds.contains(rental.getId())) {
                    sentPendingDepositReminderIds.add(rental.getId());
                    emailService.sendPendingDepositReminderEmail(email, fullName, rental.getId(), roomNames);
                }
            }

            // 3. ĐƠN ĐÃ ĐẶT CỌC (CONFIRMED) & ĐANG CHỜ KHÁCH TỚI CHECK-IN
            if (rental.getStatus() == RentalStatus.CONFIRMED && checkinTime != null && isWaitingForCheckin(rental)) {
                long minutesUntilCheckin = ChronoUnit.MINUTES.between(now, checkinTime);
                long minutesOverdueCheckin = ChronoUnit.MINUTES.between(checkinTime, now);

                // 3.1. Sắp tới giờ Check-in (còn từ 1 đến 120 phút trước giờ nhận phòng) -> Gửi mail nhắc lịch
                if (minutesUntilCheckin > 0 && minutesUntilCheckin <= 120
                        && !sentUpcomingCheckinReminderIds.contains(rental.getId())) {
                    sentUpcomingCheckinReminderIds.add(rental.getId());
                    emailService.sendUpcomingCheckinReminderEmail(
                            email, fullName, rental.getId(), roomNames, checkinTime.format(DT_FORMAT)
                    );
                }

                // 3.2. Quá giờ Check-in từ 15 đến 59 phút mà chưa vào -> Gửi mail cảnh báo chuẩn bị auto hủy
                if (minutesOverdueCheckin >= 15 && minutesOverdueCheckin < 60
                        && !sentOverdueCheckinWarningIds.contains(rental.getId())) {
                    sentOverdueCheckinWarningIds.add(rental.getId());
                    emailService.sendOverdueCheckinWarningEmail(
                            email, fullName, rental.getId(), roomNames, checkinTime.format(DT_FORMAT)
                    );
                }

                // 3.3. Quá giờ Check-in từ 60 phút trở lên mà vẫn chưa vào -> Tự động hủy No-show & Gửi mail báo hủy
                if (minutesOverdueCheckin >= 60) {
                    rental.setStatus(RentalStatus.CANCELLED);
                    if (rental.getRentalDetails() != null) {
                        rental.getRentalDetails().forEach(d -> d.setStatus(RentalDetailStatus.CANCELLED));
                    }
                    rentalRepository.save(rental);

                    emailService.sendBookingCancelledEmail(
                            email,
                            fullName,
                            rental.getId(),
                            roomNames,
                            "Quá giờ nhận phòng (" + checkinTime.format(DT_FORMAT) + ") hơn 60 phút nhưng quý khách không đến làm thủ tục Check-in (No-show)."
                    );
                }
            }
        }
    }

    private boolean isWaitingForCheckin(Rental rental) {
        if (rental.getRentalDetails() == null || rental.getRentalDetails().isEmpty()) return false;
        return rental.getRentalDetails().stream().allMatch(d -> d.getStatus() == RentalDetailStatus.PENDING);
    }

    private String extractRoomNames(Rental rental) {
        if (rental.getRentalDetails() == null || rental.getRentalDetails().isEmpty()) return "N/A";
        return rental.getRentalDetails().stream()
                .map(d -> d.getRoom() != null ? d.getRoom().getName() : "N/A")
                .collect(Collectors.joining(", "));
    }

    private LocalDateTime extractEarliestCheckin(Rental rental) {
        if (rental.getRentalDetails() == null) return null;
        return rental.getRentalDetails().stream()
                .map(RentalDetail::getCheckinDate)
                .filter(d -> d != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }
}