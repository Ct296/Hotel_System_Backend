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

    private static final int MINIMUM_STAY_HOURS = 1;

    @Transactional
    public Rental processBooking(BookingConfirmRequest request, Customer customer) {
        List<Room> selectedRooms = roomRepository.findAllById(request.getRoomId());
        if (selectedRooms.isEmpty() || selectedRooms.size() != request.getRoomId().size()) {
            throw new IllegalArgumentException("Danh sách phòng không hợp lệ.");
        }

        LocalDateTime checkIn = request.getCheckIn();
        LocalDateTime checkOut = request.getCheckOut();

        // 1. Kiểm tra trùng lịch và sức chứa cho từng phòng
        for (int i = 0; i < selectedRooms.size(); i++) {
            Room room = selectedRooms.get(i);

            if (rentalOperationService.hasBlockingConflict(room.getId(), checkIn, checkOut.plusMinutes(30), null)) {
                throw new IllegalStateException("Phòng " + room.getName() + " đã có người đặt trong khung giờ này.");
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

        // 2. Tạo Booking chính (Rental)
        Rental rental = new Rental();
        rental.setId(AppUtils.generateId("REN", 10));
        rental.setRentDate(LocalDateTime.now());
        
        String note = request.getNote() != null ? request.getNote().trim() : "";
        rental.setNote(note.isBlank() ? null : (note.length() > 255 ? note.substring(0, 255) : note));
        rental.setIsBooking(true);
        rental.setStatus(RentalStatus.PENDING);
        rental.setCustomer(customer);
        rentalRepository.save(rental);

        // 3. Tạo chi tiết cho từng phòng
        for (int i = 0; i < selectedRooms.size(); i++) {
            Room room = selectedRooms.get(i);
            RentalDetail detail = new RentalDetail();
            detail.setId(AppUtils.generateId("RDT", 10));
            detail.setCheckinDate(checkIn);
            detail.setLengthOfStay(stayHours);
            detail.setAdultCount(request.getAdultCounts().get(i));
            detail.setChildrenCount(request.getChildrenCounts().get(i));
            detail.setUnitPrice(resolveRoomUnitPrice(room, checkIn));
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
            throw new IllegalArgumentException("Chỉ áp dụng cho booking đặt trước.");
        }
        
        // Chỉ cho phép hủy khi booking đang ở trạng thái PENDING (Chờ cọc) hoặc CONFIRMED (Đã cọc/Xác nhận)
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking này không thể hủy do đang ở trạng thái không cho phép.");
        }

        // Lấy số tiền cọc đã thanh toán (nếu có)
        double depositPaid = billingService.calculateDepositPaid(rental.getId());
        boolean wasConfirmed = rental.getStatus() == RentalStatus.CONFIRMED;
                 
        // Chuyển trạng thái hóa đơn và các phòng chi tiết sang Đã Hủy (CANCELLED)
        rental.setStatus(RentalStatus.CANCELLED);
        if (rental.getRentalDetails() != null) {
            for(RentalDetail detail : rental.getRentalDetails()) {
                detail.setStatus(RentalDetailStatus.CANCELLED);
            }
        }
                 
        rentalRepository.save(rental);
        
        // Trả về số tiền cọc để hiển thị thông báo mất cọc, nếu chưa cọc thì trả về 0
        return wasConfirmed ? depositPaid : 0.0;
    }

    private double resolveRoomUnitPrice(Room room, LocalDateTime checkInDate) {
        if (room == null || room.getRoomType() == null) return 0.0;
        double basePrice = room.getRoomType().getBasePrice() == null ? 0.0 : room.getRoomType().getBasePrice();
        
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