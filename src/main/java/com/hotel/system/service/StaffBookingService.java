package com.hotel.system.service;

import com.hotel.system.dto.staff.CheckInRoomGuestsRequest;
import com.hotel.system.dto.staff.WalkInRequest;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StaffBookingService {

    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final int MINIMUM_WALK_IN_HOURS = 1;

    private final BillingService billingService;
    private final CustomerTierService customerTierService;
    private final RentalOperationService rentalOperationService;
    private final EmailService emailService;
    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RoomRepository roomRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final UsersRepository usersRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public String createWalkInRental(WalkInRequest request) {
        List<String> roomIds = request.getRoomId();
        List<Integer> adultCounts = request.getAdultCount();
        List<Integer> childrenCounts = request.getChildrenCount();

        RentalType type = request.getRentalType() != null ? request.getRentalType() : RentalType.HOURLY;
        Integer inputLength = request.getLengthOfStay();

        if (roomIds == null || roomIds.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 phòng.");
        if (inputLength == null || inputLength < 1) throw new IllegalArgumentException("Số lượng giờ/đêm tối thiểu là 1.");

        LocalDateTime checkIn = request.getCheckIn() != null ? request.getCheckIn() : LocalDateTime.now();
        if (checkIn.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Thời gian nhận phòng không được nằm trong quá khứ.");
        }

        LocalDateTime checkOut;
        int lengthOfStay;
        if (type == RentalType.DAILY) {
            checkOut = checkIn.toLocalDate().plusDays(inputLength).atTime(12, 0);
            if (checkOut.isBefore(checkIn)) checkOut = checkOut.plusDays(1);
            lengthOfStay = (int) Math.max(1, ChronoUnit.HOURS.between(checkIn, checkOut));
        } else {
            lengthOfStay = inputLength;
            checkOut = checkIn.plusHours(lengthOfStay);
        }

        for (String rId : roomIds) {
            if (rentalOperationService.hasBlockingConflict(rId, checkIn, checkOut.plusMinutes(CLEANING_BUFFER_MINUTES), null)) {
                throw new IllegalArgumentException("Phòng đã bị đặt trong khung giờ này. Vui lòng tải lại trang.");
            }
        }

        Customer customer = resolveOrCreateWalkInCustomer(request);

        Rental rental = new Rental();
        rental.setId(AppUtils.generateId("REN", 10));
        rental.setRentDate(LocalDateTime.now());
        rental.setIsBooking(true);
        rental.setStatus(RentalStatus.PENDING);
        rental.setCustomer(customer);
        rental = rentalRepository.save(rental);

        for (int i = 0; i < roomIds.size(); i++) {
            Room room = roomRepository.findById(roomIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
            RentalDetail detail = new RentalDetail();
            detail.setId(AppUtils.generateId("RDT", 10));
            detail.setRental(rental);
            detail.setRoom(room);
            detail.setCheckinDate(checkIn);
            detail.setLengthOfStay(lengthOfStay);
            detail.setRentalType(type);
            detail.setAdultCount(adultCounts.get(i));
            detail.setChildrenCount(childrenCounts.get(i));
            detail.setUnitPrice(billingService.resolveRoomUnitPrice(room, checkIn, type));
            detail.setStatus(RentalDetailStatus.PENDING);

            if (i == 0 && customer != null && customer.getUser() != null) {
                Users u = customer.getUser();
                RentalGuest repGuest = new RentalGuest();
                repGuest.setId(AppUtils.generateId("RGS", 10));
                repGuest.setFullName(AppUtils.buildFullName(u, "Khách Walk-in"));
                repGuest.setPid(u.getPid());
                repGuest.setPhoneNumber(u.getPhoneNumber());
                repGuest.setIsAdult(true);
                repGuest.setIsRepresentative(true);
                repGuest.setRentalDetail(detail);
                detail.getRentalGuests().add(repGuest);
            }
            rentalDetailRepository.save(detail);
        }
        return "Tạo đơn thành công! Đơn đang ở danh sách 'Chờ cọc'.";
    }

    @Transactional
    public String payDepositCash(String rentalId) {
        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.PENDING) {
            throw new IllegalArgumentException("Đơn không hợp lệ hoặc đã được xử lý.");
        }

        double totalDepositAmount = billingService.calculateDepositAmount(rental);

        Bill depositBill = new Bill();
        depositBill.setId(AppUtils.generateId("BIL", 10));
        depositBill.setCreateDate(LocalDateTime.now());
        depositBill.setTotalAmount(totalDepositAmount);
        depositBill.setType(BillType.DEPOSIT);
        depositBill.setRental(rental);
        billRepository.save(depositBill);

        Payment payment = new Payment();
        payment.setId(AppUtils.generateId("PAY", 10));
        payment.setMethod(PaymentMethod.CASH);
        payment.setDate(LocalDateTime.now());
        payment.setTransaction("CASH-DEP-" + rental.getId());
        payment.setBill(depositBill);
        paymentRepository.save(payment);

        rental.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental);

        if (rental.getCustomer() != null && rental.getCustomer().getUser() != null) {
            Users u = rental.getCustomer().getUser();
            String checkInStr = (!rental.getRentalDetails().isEmpty() && rental.getRentalDetails().get(0).getCheckinDate() != null)
                    ? rental.getRentalDetails().get(0).getCheckinDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "N/A";
            emailService.sendBookingConfirmedEmail(
                    u.getEmail(), AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                    rental.getId(), rentalOperationService.extractRoomNames(rental), checkInStr,
                    AppUtils.formatMoney(totalDepositAmount), "Tiền mặt tại quầy"
            );
        }
        return "Đã thu tiền cọc (Tiền mặt) thành công. Khách đã có thể Check-in.";
    }

    @Transactional
    public String cancelBooking(String rentalId) {
        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || !Boolean.TRUE.equals(rental.getIsBooking())) {
            throw new IllegalArgumentException("Không thể huỷ.");
        }

        rental.setStatus(RentalStatus.CANCELLED);
        for (RentalDetail detail : rental.getRentalDetails()) {
            detail.setStatus(RentalDetailStatus.CANCELLED);
        }
        rentalRepository.save(rental);

        if (rental.getCustomer() != null && rental.getCustomer().getUser() != null) {
            Users u = rental.getCustomer().getUser();
            emailService.sendBookingCancelledEmail(
                    u.getEmail(), AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                    rental.getId(), rentalOperationService.extractRoomNames(rental),
                    "Đơn đặt phòng được hủy bởi nhân viên lễ tân theo yêu cầu."
            );
        }
        return "Đã hủy booking thành công.";
    }

    @Transactional
    public String cancelBookingWithRefund(String rentalId, Map<String, String> payload) {
        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ áp dụng hủy hoàn cọc cho đơn đã xác nhận (CONFIRMED).");
        }

        boolean hasCheckedIn = rental.getRentalDetails().stream()
                .anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE);
        if (hasCheckedIn) {
            throw new IllegalArgumentException("Khách đã nhận phòng, vui lòng xử lý hoàn tiền tại màn hình trả phòng.");
        }

        Map<String, String> safePayload = payload != null ? payload : Map.of();
        String note = safePayload.getOrDefault("note", "Hủy đơn và hoàn 100% tiền cọc do sự cố từ khách sạn");
        double depositPaid = billingService.calculateDepositPaid(rentalId);

        if (depositPaid > 0 && !billingService.hasSettlementBill(rentalId)) {
            PaymentMethod refundMethod = PaymentMethod.valueOf(safePayload.getOrDefault("paymentMethod", "CASH"));
            Bill refundBill = new Bill();
            refundBill.setId(AppUtils.generateId("BIL", 10));
            refundBill.setCreateDate(LocalDateTime.now());
            refundBill.setTotalAmount(depositPaid);
            refundBill.setType(BillType.REFUND);
            refundBill.setNote(note);
            refundBill.setRental(rental);
            billRepository.save(refundBill);

            Payment refundPayment = new Payment();
            refundPayment.setId(AppUtils.generateId("PAY", 10));
            refundPayment.setMethod(refundMethod);
            refundPayment.setDate(LocalDateTime.now());
            String tx = safePayload.get("transactionCode");
            refundPayment.setTransaction((tx != null && !tx.isBlank()) ? tx.trim() : "REFUND-DEP-" + rental.getId());
            refundPayment.setNote(note);
            refundPayment.setBill(refundBill);
            paymentRepository.save(refundPayment);
        }

        rental.setStatus(RentalStatus.CANCELLED);
        for (RentalDetail detail : rental.getRentalDetails()) {
            if (detail.getStatus() == RentalDetailStatus.PENDING) {
                detail.setStatus(RentalDetailStatus.CANCELLED);
            }
        }
        rentalRepository.save(rental);
        customerTierService.syncCustomerTierByBills(rental.getCustomer());

        if (rental.getCustomer() != null && rental.getCustomer().getUser() != null) {
            Users u = rental.getCustomer().getUser();
            emailService.sendBookingCancelledEmail(
                    u.getEmail(), AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                    rental.getId(), rentalOperationService.extractRoomNames(rental),
                    note + (depositPaid > 0 ? " (Đã hoàn cọc: " + AppUtils.formatMoney(depositPaid) + ")" : "")
            );
        }
        return "Đã hủy đơn và hoàn tiền cọc " + AppUtils.formatMoney(depositPaid) + " cho khách.";
    }

    @Transactional
    public String checkIn(String rentalId, CheckInRoomGuestsRequest guestRequest) {
        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ được check-in booking đã xác nhận.");
        }

        List<RentalDetail> pendingDetails = rental.getRentalDetails().stream()
                .filter(d -> d.getStatus() == RentalDetailStatus.PENDING)
                .toList();

        for (RentalDetail detail : pendingDetails) {
            if (detail.getRoom() == null || detail.getRoom().getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalArgumentException("Phòng " + (detail.getRoom() != null ? detail.getRoom().getName() : "") + " hiện chưa trống.");
            }
        }

        if (guestRequest != null && guestRequest.getRoomGuests() != null && !guestRequest.getRoomGuests().isEmpty()) {
            applyGuestsOnCheckIn(pendingDetails, guestRequest);
        }

        LocalDateTime actualCheckinTime = LocalDateTime.now();
        for (RentalDetail detail : pendingDetails) {
            LocalDateTime plannedCheckin = detail.getCheckinDate();
            int plannedStayHours = (detail.getLengthOfStay() != null && detail.getLengthOfStay() > 0)
                    ? detail.getLengthOfStay() : MINIMUM_WALK_IN_HOURS;

            if (plannedCheckin != null && actualCheckinTime.isBefore(plannedCheckin)) {
                LocalDateTime plannedCheckout = plannedCheckin.plusHours(plannedStayHours);
                long minutesUntilPlannedCheckout = ChronoUnit.MINUTES.between(actualCheckinTime, plannedCheckout);
                int adjustedHours = (int) Math.max(plannedStayHours, (minutesUntilPlannedCheckout + 59) / 60);
                detail.setLengthOfStay(adjustedHours);
                detail.setCheckinDate(actualCheckinTime);
            } else if (plannedCheckin != null && actualCheckinTime.isAfter(plannedCheckin)) {
                LocalDateTime shiftedEndWithBuffer = actualCheckinTime
                        .plusHours(plannedStayHours)
                        .plusMinutes(CLEANING_BUFFER_MINUTES);

                boolean conflictIfShifted = detail.getRoom() != null
                        && rentalOperationService.hasBlockingConflict(
                                detail.getRoom().getId(),
                                actualCheckinTime,
                                shiftedEndWithBuffer,
                                rental.getId()
                        );

                if (conflictIfShifted) {
                    String lateNote = "Đến trễ lúc " + actualCheckinTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            + " (giữ nguyên giờ trả dự kiến do có đơn kế tiếp)";
                    detail.setNote(detail.getNote() == null || detail.getNote().isBlank()
                            ? lateNote
                            : detail.getNote() + " | " + lateNote);
                } else {
                    detail.setCheckinDate(actualCheckinTime);
                }
            } else {
                detail.setCheckinDate(actualCheckinTime);
            }

            detail.setStatus(RentalDetailStatus.CHECKED_IN);
            rentalDetailRepository.save(detail);
        }

        if (rental.getCustomer() != null && rental.getCustomer().getUser() != null) {
            Users u = rental.getCustomer().getUser();
            LocalDateTime expectedOut = rentalOperationService.calculateExpectedCheckout(rental);
            String outStr = expectedOut != null
                    ? expectedOut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Theo quy định";
            emailService.sendCheckInWelcomeEmail(
                    u.getEmail(), AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                    rental.getId(), rentalOperationService.extractRoomNames(rental), outStr
            );
        }
        return "Check-in và khai báo khách lưu trú thành công!";
    }

    @Transactional
    public String updateRoomGuests(String detailId, List<CheckInRoomGuestsRequest.GuestItem> guests) {
        RentalDetail detail = rentalDetailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phòng thuê."));

        String roomName = detail.getRoom() != null ? detail.getRoom().getName() : detail.getId();
        if (guests == null || guests.isEmpty()) {
            throw new IllegalArgumentException("Phòng " + roomName + " phải có ít nhất 1 khách lưu trú.");
        }

        Set<String> existingPidsInOtherRooms = new HashSet<>();
        if (detail.getRental() != null && detail.getRental().getRentalDetails() != null) {
            for (RentalDetail other : detail.getRental().getRentalDetails()) {
                if (!other.getId().equals(detail.getId()) && other.getRentalGuests() != null) {
                    for (RentalGuest rg : other.getRentalGuests()) {
                        if (rg.getPid() != null && !rg.getPid().isBlank()) existingPidsInOtherRooms.add(rg.getPid().trim());
                    }
                }
            }
        }

        validateAndReplaceRoomGuests(detail, guests, existingPidsInOtherRooms);
        rentalDetailRepository.save(detail);
        return "Đã cập nhật danh sách khách lưu trú Phòng " + roomName + ".";
    }

    // ==================== PRIVATE HELPERS ====================

    private Customer resolveOrCreateWalkInCustomer(WalkInRequest request) {
        String existingCustomerPid = request.getExistingCustomerPid();
        if (existingCustomerPid != null && !existingCustomerPid.trim().isEmpty()) {
            Users user = usersRepository.findByPid(existingCustomerPid.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với CCCD này."));
            return customerRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalArgumentException("Hồ sơ này không phải là khách hàng."));
        }

        String pid = request.getPid() != null ? request.getPid().trim() : "";
        if (pid.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập CCCD/Hộ chiếu.");

        String rawEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String emailToSave = rawEmail.isEmpty() ? null : rawEmail;

        Optional<Users> existingUserByPid = usersRepository.findByPid(pid);
        if (existingUserByPid.isPresent()) {
            Users existingUser = existingUserByPid.get();
            if (existingUser.getRole() != Role.CUSTOMER) {
                throw new IllegalArgumentException("Số CCCD này thuộc về nhân sự nội bộ, không thể tạo đơn khách hàng!");
            }
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
                existingUser.setPhoneNumber(request.getPhoneNumber().trim());
            }
            if (emailToSave != null && existingUser.getEmail() == null) {
                if (usersRepository.findByEmail(emailToSave).isPresent()) {
                    throw new IllegalArgumentException("Email này đã được sử dụng bởi một người khác!");
                }
                existingUser.setEmail(emailToSave);
            }
            existingUser.setUpdateDate(LocalDateTime.now());
            Users savedUser = usersRepository.save(existingUser);
            return customerRepository.findByUser(savedUser).orElseGet(() -> {
                Customer newCus = new Customer();
                newCus.setUser(savedUser);
                return customerRepository.save(newCus);
            });
        }

        if (emailToSave != null && usersRepository.findByEmail(emailToSave).isPresent()) {
            throw new IllegalArgumentException("Email này đã được sử dụng bởi một tài khoản khác!");
        }

        Users newUser = new Users();
        newUser.setId(AppUtils.generateId("USR", 10));
        newUser.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        newUser.setFirstName(request.getFirstName() != null ? request.getFirstName().trim() : "Khách");
        newUser.setEmail(emailToSave);
        newUser.setPid(pid);
        newUser.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "");
        newUser.setNationality(request.getNationality() != null && !request.getNationality().isBlank() ? request.getNationality().trim() : "Việt Nam");
        newUser.setSex(request.getSex() != null && !request.getSex().isBlank() ? Gender.valueOf(request.getSex().trim().toUpperCase()) : Gender.MALE);
        newUser.setDateOfBirth(request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank() ? LocalDate.parse(request.getDateOfBirth()) : LocalDate.of(2000, 1, 1));
        newUser.setRole(Role.CUSTOMER);
        newUser.setAvatar("default_avatar_customer.jpg");
        newUser.setCreateDate(LocalDateTime.now());
        newUser.setUpdateDate(LocalDateTime.now());
        newUser = usersRepository.save(newUser);

        Customer customer = new Customer();
        customer.setUser(newUser);
        customer = customerRepository.save(customer);
        customerTierService.syncCustomerTierByBills(customer);
        return customer;
    }

    private void applyGuestsOnCheckIn(List<RentalDetail> details, CheckInRoomGuestsRequest guestRequest) {
        Map<String, List<CheckInRoomGuestsRequest.GuestItem>> guestGroupMap = new HashMap<>();
        for (var group : guestRequest.getRoomGuests()) {
            if (group.getRentalDetailId() != null) {
                guestGroupMap.put(group.getRentalDetailId(), group.getGuests() != null ? group.getGuests() : List.of());
            }
        }
        Set<String> allPidsAcrossRooms = new HashSet<>();
        for (RentalDetail detail : details) {
            List<CheckInRoomGuestsRequest.GuestItem> guests = guestGroupMap.get(detail.getId());
            if (guests == null || guests.isEmpty()) {
                String roomName = detail.getRoom() != null ? detail.getRoom().getName() : detail.getId();
                throw new IllegalArgumentException("Vui lòng khai báo ít nhất 1 khách lưu trú cho Phòng " + roomName + ".");
            }
            validateAndReplaceRoomGuests(detail, guests, allPidsAcrossRooms);
        }
    }

    private void validateAndReplaceRoomGuests(RentalDetail detail,
                                              List<CheckInRoomGuestsRequest.GuestItem> guests,
                                              Set<String> usedPids) {
        String roomName = detail.getRoom() != null ? detail.getRoom().getName() : detail.getId();
        int maxAdults = (detail.getRoom() != null && detail.getRoom().getRoomType() != null && detail.getRoom().getRoomType().getAdultMax() != null)
                ? detail.getRoom().getRoomType().getAdultMax() : 10;
        int maxChildren = (detail.getRoom() != null && detail.getRoom().getRoomType() != null && detail.getRoom().getRoomType().getChildrenMax() != null)
                ? detail.getRoom().getRoomType().getChildrenMax() : 10;

        int adultCount = 0;
        int childrenCount = 0;

        for (int idx = 0; idx < guests.size(); idx++) {
            var g = guests.get(idx);
            String fullName = AppUtils.safeTrim(g.getFullName());
            String pid = AppUtils.safeTrim(g.getPid());
            boolean isAdult = g.getIsAdult() == null || g.getIsAdult();

            if (fullName.isEmpty()) throw new IllegalArgumentException("Phòng " + roomName + ": Khách thứ " + (idx + 1) + " chưa nhập Họ tên.");
            if (isAdult) {
                adultCount++;
                if (pid.isEmpty() || pid.length() < 9 || pid.length() > 20) {
                    throw new IllegalArgumentException("Phòng " + roomName + ": Người lớn (" + fullName + ") bắt buộc nhập số CCCD/Hộ chiếu (9-20 ký tự).");
                }
            } else {
                childrenCount++;
            }
            if (!pid.isEmpty() && !usedPids.add(pid)) {
                throw new IllegalArgumentException("Số CCCD/Hộ chiếu '" + pid + "' (" + fullName + ") bị trùng lặp trong đơn!");
            }
        }

        if (adultCount < 1) throw new IllegalArgumentException("Phòng " + roomName + " phải có ít nhất 1 người lớn lưu trú.");
        if (adultCount > maxAdults || childrenCount > maxChildren) {
            throw new IllegalArgumentException("Phòng " + roomName + " vượt quá sức chứa tối đa (" + maxAdults + " NL, " + maxChildren + " TE).");
        }

        detail.setAdultCount(adultCount);
        detail.setChildrenCount(childrenCount);
        detail.getRentalGuests().clear();

        for (int idx = 0; idx < guests.size(); idx++) {
            var g = guests.get(idx);
            RentalGuest entity = new RentalGuest();
            entity.setId(AppUtils.generateId("RGS", 10));
            entity.setFullName(AppUtils.safeTrim(g.getFullName()));
            String pid = AppUtils.safeTrim(g.getPid());
            String phone = AppUtils.safeTrim(g.getPhoneNumber());
            entity.setPid(pid.isEmpty() ? null : pid);
            entity.setPhoneNumber(phone.isEmpty() ? null : phone);
            entity.setIsAdult(g.getIsAdult() == null || g.getIsAdult());
            entity.setIsRepresentative(idx == 0 || Boolean.TRUE.equals(g.getIsRepresentative()));
            entity.setRentalDetail(detail);
            detail.getRentalGuests().add(entity);
        }
    }
}