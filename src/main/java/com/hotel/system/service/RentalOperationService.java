package com.hotel.system.service;

import com.hotel.system.entity.Bill;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.enums.BillType;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RentalStatus;
import com.hotel.system.entity.enums.RoomStatus;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.repository.RentalDetailRepository;
import com.hotel.system.repository.RentalRepository;
import com.hotel.system.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class RentalOperationService {
    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final long BOOKING_HOLD_MINUTES = 30;

    // Đã sửa thành RentalDetailStatus để theo dõi chi tiết phòng
    private static final Set<RentalDetailStatus> BLOCKING_BOOKING_STATUSES = Set.of(
            RentalDetailStatus.PENDING,
            RentalDetailStatus.CHECKED_IN,
            RentalDetailStatus.OVERDUE
    );

    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RoomRepository roomRepository;
    private final BillRepository billRepository;
    private final BillingService billingService;

    public RentalOperationService(RentalRepository rentalRepository,
                                  RentalDetailRepository rentalDetailRepository,
                                  RoomRepository roomRepository,
                                  BillRepository billRepository,
                                  BillingService billingService) {
        this.rentalRepository = rentalRepository;
        this.rentalDetailRepository = rentalDetailRepository;
        this.roomRepository = roomRepository;
        this.billRepository = billRepository;
        this.billingService = billingService;
    }

    public LocalDateTime calculateExpectedCheckout(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null) return null;
        return rental.getRentalDetails().stream()
                .filter(d -> d.getCheckinDate() != null && d.getLengthOfStay() != null)
                .map(d -> d.getCheckinDate().plusHours(d.getLengthOfStay()))
                .max(LocalDateTime::compareTo).orElse(null);
    }

    public boolean isNearCheckout(Rental rental, long thresholdMinutes) {
        if (rental == null) return false;
        // Kiểm tra xem có chi tiết phòng nào đang ở không
        boolean hasCheckedInRoom = rental.getRentalDetails().stream()
                .anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN);
        if (!hasCheckedInRoom) return false;

        LocalDateTime expectedCheckout = calculateExpectedCheckout(rental);
        if (expectedCheckout == null) return false;
        long remainingMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), expectedCheckout);
        return remainingMinutes >= 0 && remainingMinutes <= thresholdMinutes;
    }

    public long calculateOverdueMinutes(Rental rental) {
        LocalDateTime expectedCheckout = calculateExpectedCheckout(rental);
        if (expectedCheckout == null) return 0;
        long overdue = ChronoUnit.MINUTES.between(expectedCheckout, LocalDateTime.now());
        return Math.max(0, overdue);
    }

    public void markCheckedInRentalsAsOverdue() {
        LocalDateTime now = LocalDateTime.now();
        // Cập nhật trạng thái OVERDUE cho từng RentalDetail (từng phòng)
        List<RentalDetail> overdueDetails = rentalDetailRepository.findAll().stream()
                .filter(detail -> detail.getStatus() == RentalDetailStatus.CHECKED_IN)
                .filter(detail -> detail.getCheckinDate() != null && detail.getLengthOfStay() != null)
                .filter(detail -> now.isAfter(detail.getCheckinDate().plusHours(detail.getLengthOfStay())))
                .toList();

        for (RentalDetail detail : overdueDetails) {
            detail.setStatus(RentalDetailStatus.OVERDUE);
        }
        if (!overdueDetails.isEmpty()) {
            rentalDetailRepository.saveAll(overdueDetails);
        }
    }

    public void expirePendingBookingsWithoutDeposit() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.minusMinutes(BOOKING_HOLD_MINUTES);
        List<Rental> expiredBookings = rentalRepository.findAll().stream()
                .filter(rental -> Boolean.TRUE.equals(rental.getIsBooking()))
                .filter(rental -> rental.getStatus() == RentalStatus.PENDING)
                .filter(rental -> rental.getRentDate() != null && rental.getRentDate().isBefore(expiryThreshold))
                .filter(rental -> billingService.calculateDepositPaid(rental.getId()) <= 0)
                .toList();

        for (Rental rental : expiredBookings) {
            rental.setStatus(RentalStatus.CANCELLED);
            if (rental.getRentalDetails() != null) {
                for(RentalDetail detail : rental.getRentalDetails()) {
                    detail.setStatus(RentalDetailStatus.CANCELLED);
                }
            }
        }
        if (!expiredBookings.isEmpty()) {
            rentalRepository.saveAll(expiredBookings);
        }
    }

    public boolean hasBlockingConflict(String roomId, LocalDateTime requestedStart, LocalDateTime requestedEndWithBuffer, String ignoreRentalId) {
        // Dùng hàm repository mới gọi theo RentalDetailStatus
        return rentalRepository.findByRentalDetails_RoomIdAndRentalDetails_StatusIn(roomId, BLOCKING_BOOKING_STATUSES).stream()
                .filter(existing -> ignoreRentalId == null || !existing.getId().equals(ignoreRentalId))
                .filter(existing -> existing.getRentalDetails() != null && !existing.getRentalDetails().isEmpty()
                                   && existing.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(existing -> existing.getRentalDetails().get(0).getLengthOfStay() != null && existing.getRentalDetails().get(0).getLengthOfStay() > 0)
                .anyMatch(existing -> {
                    LocalDateTime existingStart = existing.getRentalDetails().get(0).getCheckinDate();
                    LocalDateTime existingEndWithBuffer = calculateRentalEndWithBuffer(existing);
                    return existingEndWithBuffer != null
                            && existingStart.isBefore(requestedEndWithBuffer)
                            && existingEndWithBuffer.isAfter(requestedStart);
                });
    }

    private LocalDateTime calculateRentalEndWithoutBuffer(Rental rental) {
        RentalDetail firstDetail = (rental != null && rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty())
                                     ? rental.getRentalDetails().get(0) : null;
        if (firstDetail == null || firstDetail.getCheckinDate() == null || firstDetail.getLengthOfStay() == null || firstDetail.getLengthOfStay() <= 0) {
            return null;
        }
        return firstDetail.getCheckinDate().plusHours(firstDetail.getLengthOfStay());
    }

    public LocalDateTime calculateRentalEndWithBuffer(Rental rental) {
        LocalDateTime endWithoutBuffer = calculateRentalEndWithoutBuffer(rental);
        return endWithoutBuffer == null ? null : endWithoutBuffer.plusMinutes(CLEANING_BUFFER_MINUTES);
    }

    public void autoReleaseCleaningRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> roomsToRelease = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.CLEANING)
                .filter(room -> room.getId() != null)
                .filter(room -> {
                    LocalDateTime latestCheckoutTime = findLatestCheckoutTime(room.getId());
                    return latestCheckoutTime != null && !now.isBefore(latestCheckoutTime.plusMinutes(CLEANING_BUFFER_MINUTES));
                })
                .toList();

        if (roomsToRelease.isEmpty()) return;
        for (Room room : roomsToRelease) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        roomRepository.saveAll(roomsToRelease);
    }

    private LocalDateTime findLatestCheckoutTime(String roomId) {
        return billRepository.findAllByOrderByCreateDateDesc().stream()
                .filter(bill -> bill.getType() == BillType.FINAL || bill.getType() == BillType.EARLY_CHECKOUT)
                .filter(bill -> bill.getRental() != null)
                .filter(bill -> bill.getRental().getRentalDetails() != null && !bill.getRental().getRentalDetails().isEmpty()
                                 && bill.getRental().getRentalDetails().get(0).getRoom() != null)
                .filter(bill -> roomId.equals(bill.getRental().getRentalDetails().get(0).getRoom().getId()))
                .map(Bill::getCreateDate)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}