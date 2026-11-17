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
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomerBookingService {

    private final RoomRepository roomRepository;
    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RentalGuestRepository rentalGuestRepository;
    private final RentalOperationService rentalOperationService;
    private final BillingService billingService;
    private final EmailService emailService;
    
    private final AccountStatusRepository accountStatusRepository;
    private final AccountRepository accountRepository;

    private static final int MINIMUM_STAY_HOURS = 1;

    @Transactional
    public Rental processBooking(BookingConfirmRequest request, Customer customer) {
        // KIỂM TRA PHẠT 1: Tài khoản có đang bị khóa đặt phòng không
        AccountStatus latestStatus = accountStatusRepository.findTopByAccountIdOrderByStartTimeDesc(customer.getId()).orElse(null);
        if (latestStatus != null && (latestStatus.getName() == AccountState.LOCKED || latestStatus.getName() == AccountState.BOOKING_BLOCKED)) {
            if (latestStatus.getEndTime() == null || latestStatus.getEndTime().isAfter(LocalDateTime.now())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                String timeStr = latestStatus.getEndTime() == null ? "Vĩnh viễn" : latestStatus.getEndTime().format(formatter);
                String reason = latestStatus.getReason() != null ? latestStatus.getReason() : "Vi phạm chính sách hệ thống.";
                
                throw new IllegalStateException("Tính năng đặt phòng đang bị khóa!|Lý do: " + reason + "|Mở khóa vào: " + timeStr + "|Vui lòng liên hệ Hotline 0901 000 001 để được hỗ trợ.");
            }
        }

        // KIỂM TRA PHẠT 2: Tối đa 3 đơn đang chờ thanh toán
        long pendingCount = rentalRepository.findByCustomerId(customer.getId()).stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING)
                .count();
        if (pendingCount >= 3) {
            throw new IllegalStateException("Giới hạn chờ thanh toán!|Bạn đang có " + pendingCount + " đơn chờ thanh toán.|Vui lòng thanh toán hoặc hủy bớt trước khi đặt mới.|Cần hỗ trợ gọi Hotline 0901 000 001.");
        }

        List<String> requestedRoomIds = request.getRoomId();
        Map<String, Room> roomMap = new HashMap<>();
        for (Room r : roomRepository.findAllById(requestedRoomIds)) {
            roomMap.put(r.getId(), r);
        }
        if (roomMap.size() != requestedRoomIds.size()) {
            throw new IllegalArgumentException("Danh sách phòng không hợp lệ.");
        }

        // Giữ đúng thứ tự phòng theo request.getRoomId()
        List<Room> selectedRooms = requestedRoomIds.stream().map(roomMap::get).toList();

        LocalDateTime checkIn = request.getCheckIn();
        LocalDateTime checkOut = request.getCheckOut();

        // KIỂM TRA LOGIC ĐẶT CHO BẢN THÂN / ĐẶT HỘ TỪNG PHÒNG
        Users bookerUser = customer.getUser();
        String bookerFullName = AppUtils.buildFullName(bookerUser, "Khách đặt phòng");
        String bookerPid = bookerUser != null && bookerUser.getPid() != null ? bookerUser.getPid().trim() : "";
        String bookerPhone = bookerUser != null && bookerUser.getPhoneNumber() != null ? bookerUser.getPhoneNumber().trim() : "";

        int selfRoomCount = 0;
        Set<String> usedPidsInOrder = new HashSet<>();

        for (int i = 0; i < selectedRooms.size(); i++) {
            Room room = selectedRooms.get(i);

            if (rentalOperationService.hasBlockingConflict(room.getId(), checkIn, checkOut.plusMinutes(30), null)) {
                throw new IllegalStateException("Phòng " + room.getName() + " đã có lịch đặt trong khung giờ này.");
            }

            int adults = request.getAdultCounts().get(i);
            int children = request.getChildrenCounts().get(i);
            int maxAdults = room.getRoomType().getAdultMax() != null ? room.getRoomType().getAdultMax() : 0;
            int maxChildren = room.getRoomType().getChildrenMax() != null ? room.getRoomType().getChildrenMax() : 0;

            if (adults < 1 || adults > maxAdults || children < 0 || children > maxChildren) {
                throw new IllegalArgumentException("Số lượng khách không hợp lệ cho phòng " + room.getName());
            }

            // Mặc định: nếu không truyền isForSelfList thì phòng đầu tiên (i == 0) là cho bản thân, các phòng sau là đặt hộ
            boolean isForSelf = (request.getIsForSelfList() != null && i < request.getIsForSelfList().size())
                    ? Boolean.TRUE.equals(request.getIsForSelfList().get(i))
                    : (i == 0);

            if (isForSelf) {
                selfRoomCount++;
                if (selfRoomCount > 1) {
                    throw new IllegalArgumentException("Bạn chỉ có thể đăng ký bản thân lưu trú tại tối đa 1 phòng. Các phòng còn lại vui lòng chọn 'Đặt cho người đi cùng / Đặt hộ'.");
                }
                if (!bookerPid.isEmpty() && !bookerPid.startsWith("CHUA_CAP_NHAT_")) {
                    usedPidsInOrder.add(bookerPid);
                }
            } else {
                String gName = (request.getGuestFullNames() != null && i < request.getGuestFullNames().size())
                        ? AppUtils.safeTrim(request.getGuestFullNames().get(i)) : "";
                String gPhone = (request.getGuestPhones() != null && i < request.getGuestPhones().size())
                        ? AppUtils.safeTrim(request.getGuestPhones().get(i)) : "";
                String gPid = (request.getGuestPids() != null && i < request.getGuestPids().size())
                        ? AppUtils.safeTrim(request.getGuestPids().get(i)) : "";

                if (gName.isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng nhập Họ tên người nhận phòng cho Phòng " + room.getName() + ".");
                }
                if (gPhone.isEmpty() || !gPhone.matches("^\\d{9,11}$")) {
                    throw new IllegalArgumentException("Số điện thoại người nhận Phòng " + room.getName() + " phải từ 9 - 11 chữ số.");
                }
                if (!gPid.isEmpty()) {
                    if (gPid.length() < 9 || gPid.length() > 20) {
                        throw new IllegalArgumentException("CCCD/Hộ chiếu người nhận Phòng " + room.getName() + " phải từ 9 - 20 ký tự.");
                    }
                    if (!usedPidsInOrder.add(gPid)) {
                        throw new IllegalArgumentException("Số CCCD/Hộ chiếu '" + gPid + "' bị trùng lặp giữa các phòng trong cùng đơn đặt!");
                    }
                }
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
            detail.setUnitPrice(billingService.resolveRoomUnitPrice(room, checkIn, type));
            detail.setStatus(RentalDetailStatus.PENDING);
            detail.setRoom(room);
            detail.setRental(rental);

            // Tạo bản ghi RentalGuest đại diện cho phòng này
            boolean isForSelf = (request.getIsForSelfList() != null && i < request.getIsForSelfList().size())
                    ? Boolean.TRUE.equals(request.getIsForSelfList().get(i))
                    : (i == 0);

            RentalGuest representativeGuest = new RentalGuest();
            representativeGuest.setId(AppUtils.generateId("RGS", 10));
            representativeGuest.setIsAdult(true);
            representativeGuest.setIsRepresentative(true);
            representativeGuest.setRentalDetail(detail);

            if (isForSelf) {
                representativeGuest.setFullName(bookerFullName);
                representativeGuest.setPid(bookerPid.isEmpty() ? null : bookerPid);
                representativeGuest.setPhoneNumber(bookerPhone.isEmpty() ? null : bookerPhone);
            } else {
                String gName = AppUtils.safeTrim(request.getGuestFullNames().get(i));
                String gPhone = AppUtils.safeTrim(request.getGuestPhones().get(i));
                String gPid = (request.getGuestPids() != null && i < request.getGuestPids().size())
                        ? AppUtils.safeTrim(request.getGuestPids().get(i)) : "";

                representativeGuest.setFullName(gName);
                representativeGuest.setPhoneNumber(gPhone);
                representativeGuest.setPid(gPid.isEmpty() ? null : gPid);
            }

            // CHỈ CẦN ADD VÀO LIST CỦA DETAIL, CASCADE SẼ TỰ LƯU (BỎ DÒNG rentalGuestRepository.save)
            detail.getRentalGuests().add(representativeGuest);
            detail = rentalDetailRepository.save(detail);
            rental.getRentalDetails().add(detail);
        }

        // Gửi mail thông báo đã tạo đơn (nhắc thanh toán cọc trong 15p)
        if (customer.getUser() != null && customer.getUser().getEmail() != null) {
            String roomNames = selectedRooms.stream().map(Room::getName).collect(java.util.stream.Collectors.joining(", "));
            String checkInStr = checkIn.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
            double estDeposit = billingService.calculateDepositAmount(rental);
            emailService.sendBookingCreatedPendingDepositEmail(
                    customer.getUser().getEmail(),
                    AppUtils.buildFullName(customer.getUser().getFirstName(), customer.getUser().getLastName()),
                    rental.getId(),
                    roomNames,
                    checkInStr,
                    AppUtils.formatMoney(estDeposit)
            );
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
            throw new IllegalArgumentException("Chỉ hỗ trợ hủy cho booking đặt trước.");
        }
        
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking này không thể hủy do đang ở trạng thái không cho phép.");
        }

        // CHẶN HỦY NẾU KHÁCH ĐÃ CHECK-IN (ĐANG Ở) HOẶC ĐÃ TRẢ PHÒNG
        if (rental.getRentalDetails() != null) {
            boolean hasCheckedInOrProcessed = rental.getRentalDetails().stream()
                    .anyMatch(d -> d.getStatus() != null && d.getStatus() != RentalDetailStatus.PENDING);
            if (hasCheckedInOrProcessed) {
                throw new IllegalArgumentException("Bạn đã nhận phòng, không thể hủy booking. Vui lòng liên hệ lễ tân nếu muốn trả phòng sớm!");
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
            status.setReason("Hủy đơn hàng loạt 3 lần trở lên trong vòng 24 giờ.");
            
            Account account = accountRepository.findById(customerId).orElseThrow();
            status.setAccount(account);
            accountStatusRepository.save(status);

            if (account.getUser() != null && account.getUser().getEmail() != null) {
                String unlockStr = status.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
                emailService.sendSpamCancelPenaltyEmail(
                        account.getUser().getEmail(),
                        AppUtils.buildFullName(account.getUser().getFirstName(), account.getUser().getLastName()),
                        unlockStr
                );
            }
        }
    }
}