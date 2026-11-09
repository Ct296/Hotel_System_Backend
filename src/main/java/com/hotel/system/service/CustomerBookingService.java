package com.hotel.system.service;

import com.hotel.system.dto.customer.BookingConfirmRequest;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerBookingService {

    private final RoomRepository roomRepository;
    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RentalOperationService rentalOperationService;
    private final BillingService billingService;
    private final AppliedPeriodRepository appliedPeriodRepository;
    
    private final AccountStatusRepository accountStatusRepository;
    private final AccountRepository accountRepository;

    private static final int MINIMUM_STAY_HOURS = 1;

    @Transactional
    public Rental processBooking(BookingConfirmRequest request, Customer customer) {
        // KIỂM TRA PHẠT 1: Tài khoản có đang bị cấm đặt phòng không?
        AccountStatus latestStatus = accountStatusRepository.findTopByAccountIdOrderByStartTimeDesc(customer.getId()).orElse(null);
        if (latestStatus != null && (latestStatus.getName() == AccountState.LOCKED || latestStatus.getName() == AccountState.BOOKING_BLOCKED)) {
            if (latestStatus.getEndTime() == null || latestStatus.getEndTime().isAfter(LocalDateTime.now())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                String timeStr = latestStatus.getEndTime() == null ? "Vĩnh viễn" : latestStatus.getEndTime().format(formatter);
                String reason = latestStatus.getReason() != null ? latestStatus.getReason() : "Vi phạm chính sách hệ thống.";
                
                throw new IllegalStateException("Tính năng đặt phòng đang bị khóa!|Lý do: " + reason + "|Hết hạn vào: " + timeStr + "|Vui lòng liên hệ Hotline 0901 000 001 để được hỗ trợ.");
            }
        }

        // KIỂM TRA PHẠT 2: Tối đa 3 đơn đang chờ thanh toán
        long pendingCount = rentalRepository.findByCustomerId(customer.getId()).stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING)
                .count();
        if (pendingCount >= 3) {
            throw new IllegalStateException("Giới hạn chống đầu cơ phòng!|Bạn đang có " + pendingCount + " đơn chờ thanh toán.|Vui lòng thanh toán hoặc hủy các đơn cũ trước khi tạo mới.|Cần hỗ trợ? Gọi Hotline 0901 000 001.");
        }

        List<Room> selectedRooms = roomRepository.findAllById(request.getRoomId());
        if (selectedRooms.isEmpty() || selectedRooms.size() != request.getRoomId().size()) {
            throw new IllegalArgumentException("Danh sách phòng không hợp lệ.");
        }

        LocalDateTime checkIn = request.getCheckIn();
        LocalDateTime checkOut = request.getCheckOut();

        for (int i = 0; i < selectedRooms.size(); i++) {
            Room room = selectedRooms.get(i);
            if (rentalOperationService.hasBlockingConflict(room.getId(), checkIn, checkOut.plusMinutes(30), null)) {
                throw new IllegalStateException("Phòng " + room.getName() + " đã được đặt trong khung giờ này.");
            }

            int adults = request.getAdultCounts().get(i);
            int children = request.getChildrenCounts().get(i);
            int maxAdults = room.getRoomType().getAdultMax() != null ? room.getRoomType().getAdultMax() : 0;
            int maxChildren = room.getRoomType().getChildrenMax() != null ? room.getRoomType().getChildrenMax() : 0;
            if (adults < 1 || adults > maxAdults || children < 0 || children > maxChildren) {
                throw new IllegalArgumentException("Số lượng khách không hợp lệ cho phòng " + room.getName());
            }
        }

        int stayHours = (int) Math.max(ChronoUnit.HOURS.between(checkIn, checkOut), MINIMUM_STAY_HOURS);
        RentalType type = request.getRentalType() != null ? request.getRentalType() : RentalType.HOURLY;

        Rental rental = new Rental();
        rental.setId(AppUtils.generateId("REN", 10));
        rental.setRentDate(LocalDateTime.now());
        
        String note = request.getNote() != null ? request.getNote().trim() : "";
        rental.setNote(note.isBlank() ? null : (note.length() > 255 ? note.substring(0, 255) : note));
        rental.setIsBooking(true);
        rental.setStatus(RentalStatus.PENDING);
        rental.setCustomer(customer);
        rentalRepository.save(rental);

        for (int i = 0; i < selectedRooms.size(); i++) {
            Room room = selectedRooms.get(i);
            RentalDetail detail = new RentalDetail();
            detail.setId(AppUtils.generateId("RDT", 10));
            detail.setCheckinDate(checkIn);
            detail.setLengthOfStay(stayHours);
            detail.setRentalType(type); 
            
            detail.setAdultCount(request.getAdultCounts().get(i));
            detail.setChildrenCount(request.getChildrenCounts().get(i));
            detail.setUnitPrice(resolveRoomUnitPrice(room, checkIn, type));
            detail.setStatus(RentalDetailStatus.PENDING);
            detail.setRoom(room);
            detail.setRental(rental);
            rentalDetailRepository.save(detail);
        }

        return rental;
    }

    @Transactional
    public double cancelBooking(String rentalId, String customerId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking."));
                
        if (rental.getCustomer() == null || !rental.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác booking này.");
        }
        
        if (!Boolean.TRUE.equals(rental.getIsBooking())) {
            throw new IllegalArgumentException("Chức năng chỉ dành cho booking đặt trước.");
        }
        
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking này không thể hủy do đang ở trạng thái không cho phép.");
        }

        // CHẶN HỦY NẾU KHÁCH ĐÃ CHECK-IN (ĐANG Ở) HOẶC ĐÃ TRẢ PHÒNG
        if (rental.getRentalDetails() != null) {
            boolean hasCheckedInOrProcessed = rental.getRentalDetails().stream()
                    .anyMatch(d -> d.getStatus() != null && d.getStatus() != RentalDetailStatus.PENDING);
            if (hasCheckedInOrProcessed) {
                throw new IllegalArgumentException("Bạn đã nhận phòng, không thể hủy booking. Vui lòng liên hệ quầy lễ tân nếu muốn trả phòng sớm!");
            }
        }

        double depositPaid = billingService.calculateDepositPaid(rental.getId());
        boolean wasConfirmed = rental.getStatus() == RentalStatus.CONFIRMED;

        rental.setStatus(RentalStatus.CANCELLED);
        if (rental.getRentalDetails() != null) {
            for(RentalDetail detail : rental.getRentalDetails()) {
                detail.setStatus(RentalDetailStatus.CANCELLED);
            }
        }
        rentalRepository.save(rental);
        
        checkAndPenalizeSpamCancel(customerId);
        
        return wasConfirmed ? depositPaid : 0.0;
    }

    public void checkAndPenalizeSpamCancel(String customerId) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusDays(1);
        
        long cancelCount = rentalRepository.findByCustomerId(customerId).stream()
                .filter(r -> r.getStatus() == RentalStatus.CANCELLED && r.getRentDate().isAfter(twentyFourHoursAgo))
                .count();
                
        if (cancelCount >= 3) {
            AccountStatus currentStatus = accountStatusRepository.findTopByAccountIdOrderByStartTimeDesc(customerId).orElse(null);
            if (currentStatus != null && currentStatus.getName() == AccountState.BOOKING_BLOCKED && currentStatus.getEndTime() != null && currentStatus.getEndTime().isAfter(LocalDateTime.now())) {
                return;
            }

            AccountStatus status = new AccountStatus();
            status.setId(AppUtils.generateId("AST", 10));
            status.setName(AccountState.BOOKING_BLOCKED);
            status.setStartTime(LocalDateTime.now());
            status.setEndTime(LocalDateTime.now().plusDays(1)); // Khóa 1 ngày
            status.setReason("Hủy đơn hoặc để quá hạn cọc từ 3 lần trở lên trong vòng 24 giờ.");
            
            Account account = accountRepository.findById(customerId).orElseThrow();
            status.setAccount(account);
            accountStatusRepository.save(status);
        }
    }

    private double resolveRoomUnitPrice(Room room, LocalDateTime checkInDate, RentalType rentalType) {
        if (room == null || room.getRoomType() == null) return 0.0;
        
        double basePrice = 0.0;
        if (rentalType == RentalType.DAILY) {
            basePrice = room.getRoomType().getDailyPrice() != null ? room.getRoomType().getDailyPrice() : 
                        (room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() * 22 : 0.0);
        } else {
            basePrice = room.getRoomType().getBasePrice() == null ? 0.0 : room.getRoomType().getBasePrice();
        }
        
        AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(room.getRoomType().getId())
                .stream()
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !checkInDate.isBefore(p.getStartDate()) && !checkInDate.isAfter(p.getEndDate()))
                .findFirst()
                .orElse(null);

        if (matchedPeriod != null && matchedPeriod.getPriceRate() != null && matchedPeriod.getPriceRate().getSurchargeAmount() != null) {
            return basePrice + matchedPeriod.getPriceRate().getSurchargeAmount();
        }
        return basePrice;
    }
}