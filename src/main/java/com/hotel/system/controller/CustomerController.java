package com.hotel.system.controller;

import com.hotel.system.dto.customer.BookingConfirmRequest;
import com.hotel.system.dto.profile.ChangePasswordRequest;
import com.hotel.system.dto.profile.ProfileUpdateRequest;
import com.hotel.system.dto.vnpay.VnpayPaymentResult;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.security.SecurityUtils;
import com.hotel.system.service.*;
import com.hotel.system.service.CustomerHelperService.HomeRoomTypeView;
import com.hotel.system.service.CustomerHelperService.RoomCardView;
import com.hotel.system.util.AppUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CustomerController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${hotel.stay.minimum-hours:1}")
    private int minimumStayHours;

    @Value("${payment.vnpay.frontend-return-url:https://ithotel.ct296.id.vn/customer/profile}")
    private String frontendReturnUrl;

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final TierHistoryRepository tierHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;
    private final AppliedPeriodRepository appliedPeriodRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final EmailService emailService;

    private final VnpayService vnpayService;
    private final CustomerBookingService customerBookingService;
    private final CustomerHelperService customerHelperService;
    private final ProfileService profileService;
    private final RentalOperationService rentalOperationService;
    private final CustomerTierService customerTierService;
    private final SecurityUtils securityUtils;

    private Users getLoggedInCustomer() {
        return securityUtils.getCurrentUserWithRole(Role.CUSTOMER);
    }

    @GetMapping("/api/public/home")
    public ResponseEntity<?> home() {
        LocalDateTime now = LocalDateTime.now();

        List<HomeRoomTypeView> roomTypeViews = roomTypeRepository.findAll().stream()
            .map(roomType -> {
                double basePrice = customerHelperService.getSafeBasePrice(roomType);

                AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(roomType.getId())
                        .stream()
                        .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                        .filter(p -> !now.isBefore(p.getStartDate()) && !now.isAfter(p.getEndDate()))
                        .findFirst()
                        .orElse(null);

                double surcharge = (matchedPeriod != null && matchedPeriod.getPriceRate() != null && matchedPeriod.getPriceRate().getSurchargeAmount() != null)
                        ? matchedPeriod.getPriceRate().getSurchargeAmount() : 0.0;

                double actualSurchargeMoney = basePrice * (surcharge / 100.0);

                String eventName = (matchedPeriod != null && matchedPeriod.getPriceRate() != null)
                        ? matchedPeriod.getPriceRate().getEventName() : null;

                return new HomeRoomTypeView(roomType, basePrice + actualSurchargeMoney, basePrice, eventName, actualSurchargeMoney);
            }).toList();

        List<HomeRoomTypeView> roomTypesByPriceDesc = roomTypeViews.stream()
                .sorted(Comparator.comparing(HomeRoomTypeView::basePrice).reversed()
                .thenComparing(item -> item.roomType().getName(), Comparator.nullsLast(String::compareToIgnoreCase))).toList();

        List<HomeRoomTypeView> roomTypesByPriceAsc = roomTypeViews.stream()
                .sorted(Comparator.comparing(HomeRoomTypeView::basePrice)
                .thenComparing(item -> item.roomType().getName(), Comparator.nullsLast(String::compareToIgnoreCase))).toList();

        List<HomeRoomTypeView> luxuryRoomTypes = roomTypesByPriceDesc.stream().limit(3).toList();
        Set<String> luxuryRoomTypeIds = luxuryRoomTypes.stream().map(item -> item.roomType().getId()).filter(StringUtils::hasText).collect(Collectors.toSet());
        List<HomeRoomTypeView> budgetRoomTypes = roomTypesByPriceAsc.stream().filter(item -> !luxuryRoomTypeIds.contains(item.roomType().getId())).limit(3).toList();

        List<Map<String, Object>> responseLuxury = new ArrayList<>();
        for (HomeRoomTypeView view : luxuryRoomTypes) {
            RoomType rt = view.roomType();
            Map<String, Object> r = new HashMap<>();
            r.put("id", rt.getId());
            r.put("name", rt.getName());
            r.put("bed", rt.getBed());
            r.put("description", rt.getDescription());
            r.put("adultMax", rt.getAdultMax());
            r.put("childrenMax", rt.getChildrenMax());
            r.put("area", rt.getArea());
            r.put("dailyPrice", rt.getDailyPrice());
            r.put("roomAmenities", rt.getRoomAmenities());
            r.put("displayPrice", view.displayPrice());
            r.put("basePrice", view.basePrice());
            r.put("eventName", view.eventName());
            r.put("surchargeAmount", view.surchargeAmount());

            List<String> typeImages = roomRepository.findByRoomTypeId(rt.getId()).stream()
                    .flatMap(room -> customerHelperService.getRoomGalleryPaths(room).stream())
                    .filter(img -> img != null && !img.contains("default_room.jpg"))
                    .distinct()
                    .collect(Collectors.toList());
            if (typeImages.isEmpty()) {
                typeImages.add("/image/default_room.jpg");
            }
            r.put("images", typeImages);

            responseLuxury.add(r);
        }

        return ResponseEntity.ok(Map.of(
            "roomTypes", roomTypeRepository.findAll().stream().sorted(Comparator.comparing(RoomType::getName)).toList(),
            "luxuryRoomTypes", responseLuxury,
            "budgetRoomTypes", budgetRoomTypes
        ));
    }

    @GetMapping("/api/public/rooms")
    public ResponseEntity<?> rooms(@RequestParam(required = false) String typeId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
                                   @RequestParam(required = false, defaultValue = "default") String sortBy) {
        rentalOperationService.expirePendingBookingsWithoutDeposit();

        LocalDateTime minimumBookingStartTime = customerHelperService.getMinimumBookingStartTime();
        LocalDateTime adjustedCheckIn = checkIn != null ? checkIn : minimumBookingStartTime;
        LocalDateTime adjustedCheckOut = checkOut != null ? checkOut : adjustedCheckIn.plusHours(1);

        if (adjustedCheckIn.isBefore(minimumBookingStartTime)) adjustedCheckIn = minimumBookingStartTime;
        if (!adjustedCheckOut.isAfter(adjustedCheckIn)) adjustedCheckOut = adjustedCheckIn.plusHours(1);

        final LocalDateTime searchCheckIn = adjustedCheckIn;
        final LocalDateTime searchCheckOut = adjustedCheckOut;

        List<RoomCardView> roomCards = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> typeId == null || typeId.isBlank() || (room.getRoomType() != null && typeId.equals(room.getRoomType().getId())))
                .filter(room -> customerHelperService.isRoomAvailable(room, searchCheckIn, searchCheckOut))
                .map(room -> {
                    double basePrice = room.getRoomType() != null && room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() : 0.0;

                    AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(room.getRoomType().getId())
                            .stream()
                            .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                            .filter(p -> !searchCheckIn.isBefore(p.getStartDate()) && !searchCheckIn.isAfter(p.getEndDate()))
                            .findFirst()
                            .orElse(null);

                    double surcharge = (matchedPeriod != null && matchedPeriod.getPriceRate() != null && matchedPeriod.getPriceRate().getSurchargeAmount() != null)
                             ? matchedPeriod.getPriceRate().getSurchargeAmount() : 0.0;

                    double actualSurchargeMoney = basePrice * (surcharge / 100.0);

                    String eventName = (matchedPeriod != null && matchedPeriod.getPriceRate() != null)
                             ? matchedPeriod.getPriceRate().getEventName() : null;
                    return new RoomCardView(room, basePrice + actualSurchargeMoney, room.getRoomType() != null ? room.getRoomType().getName() : "", basePrice, eventName, actualSurchargeMoney);
                })
                .sorted(customerHelperService.resolveRoomSort(sortBy)).toList();

        List<Map<String, Object>> responseRooms = new ArrayList<>();
        for (RoomCardView view : roomCards) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", view.room().getId());
            r.put("name", view.room().getName());
            r.put("roomType", view.room().getRoomType());
            r.put("displayPrice", view.displayPrice());
            r.put("images", customerHelperService.getRoomGalleryPaths(view.room()));
            r.put("basePrice", view.basePrice());
            r.put("eventName", view.eventName());
            r.put("surchargeAmount", view.surchargeAmount());
            responseRooms.add(r);
        }

        return ResponseEntity.ok(Map.of(
            "roomTypes", roomTypeRepository.findAll().stream().sorted(Comparator.comparing(RoomType::getName)).toList(),
            "rooms", responseRooms,
            "checkIn", searchCheckIn,
            "checkOut", searchCheckOut
        ));
    }

    @GetMapping("/api/public/services")
    public ResponseEntity<?> services() {
        return ResponseEntity.ok(serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE));
    }

    @GetMapping("/api/public/feedback")
    public ResponseEntity<?> getAllFeedback() {
        return ResponseEntity.ok(reviewRepository.findAllByOrderByUpdateDateDesc());
    }

    @PostMapping("/api/customer/booking/calculate")
    public ResponseEntity<?> calculateBooking(@RequestBody Map<String, Object> payload) {
        List<String> roomIds = (List<String>) payload.get("roomId");
        if (roomIds == null || roomIds.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn ít nhất 1 phòng."));

        LocalDateTime checkIn = payload.get("checkIn") != null ? LocalDateTime.parse(payload.get("checkIn").toString()) : null;
        LocalDateTime checkOut = payload.get("checkOut") != null ? LocalDateTime.parse(payload.get("checkOut").toString()) : null;

        RentalType rentalType = payload.get("rentalType") != null ?
                RentalType.valueOf(payload.get("rentalType").toString()) : RentalType.HOURLY;

        List<Room> selectedRooms = roomRepository.findAllById(roomIds);
        if (selectedRooms.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy phòng."));

        LocalDateTime minimumBookingStartTime = customerHelperService.getMinimumBookingStartTime();
        LocalDateTime finalCheckIn = checkIn != null ? checkIn : minimumBookingStartTime;
        LocalDateTime finalCheckOut = checkOut != null ? checkOut : finalCheckIn.plusHours(1);
        if (finalCheckIn.isBefore(minimumBookingStartTime)) finalCheckIn = minimumBookingStartTime;
        if (!finalCheckOut.isAfter(finalCheckIn)) finalCheckOut = finalCheckIn.plusHours(1);

        double totalRoomUnitPrice = 0;
        double originalRoomUnitPrice = 0;
        boolean bookingAvailable = true;
        String conflictMessage = null;

        for (Room room : selectedRooms) {
            double basePrice = 0.0;
            if (rentalType == RentalType.DAILY) {
                basePrice = room.getRoomType() != null && room.getRoomType().getDailyPrice() != null ?
                            room.getRoomType().getDailyPrice() :
                            (room.getRoomType() != null && room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() * 22 : 0.0);
            } else {
                basePrice = room.getRoomType() != null && room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() : 0.0;
            }

            originalRoomUnitPrice += basePrice;
            totalRoomUnitPrice += customerHelperService.resolveRoomUnitPrice(room, finalCheckIn, rentalType);

            Rental conflictRental = customerHelperService.findBlockingRentalConflict(room, finalCheckIn, finalCheckOut);
            if (conflictRental != null && bookingAvailable) {
                bookingAvailable = false;
                conflictMessage = "Phòng " + room.getName() + " đang bận lịch trong khung: " + customerHelperService.buildConflictRange(conflictRental);
            }
        }

        int stayHours = customerHelperService.calculateStayHours(finalCheckIn, finalCheckOut);

        double originalRoomSubTotal = customerHelperService.calculateRoomSubTotal(originalRoomUnitPrice, finalCheckIn, finalCheckOut, rentalType);
        double roomSubTotal = customerHelperService.calculateRoomSubTotal(totalRoomUnitPrice, finalCheckIn, finalCheckOut, rentalType);
        double eventSurchargeAmount = roomSubTotal - originalRoomSubTotal;
        double totalPrice = roomSubTotal;

        double totalDepositAmount = 0;
        for (Room room : selectedRooms) {
            double singleSub = customerHelperService.calculateRoomSubTotal(
                    customerHelperService.resolveRoomUnitPrice(room, finalCheckIn, rentalType),
                    finalCheckIn, finalCheckOut, rentalType);
            totalDepositAmount += customerHelperService.calculateDepositAmount(singleSub, customerHelperService.resolveDepositPercent(room));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", selectedRooms);
        response.put("checkIn", finalCheckIn);
        response.put("checkOut", finalCheckOut);
        response.put("stayHours", stayHours);
        response.put("originalRoomSubTotal", originalRoomSubTotal);
        response.put("eventSurchargeAmount", eventSurchargeAmount);
        response.put("shortStaySurchargeAmount", 0.0);
        response.put("totalPrice", totalPrice);
        response.put("depositAmount", totalDepositAmount);
        response.put("remainingAmount", totalPrice - totalDepositAmount);
        response.put("bookingAvailable", bookingAvailable);
        response.put("conflictMessage", conflictMessage != null ? conflictMessage : "");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/customer/booking/confirm")
    public ResponseEntity<?> confirmBooking(@Valid @RequestBody BookingConfirmRequest request, BindingResult bindingResult) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        rentalOperationService.expirePendingBookingsWithoutDeposit();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            Customer customer = customerRepository.findById(currentUser.getId()).orElseThrow();
            Rental rental = customerBookingService.processBooking(request, customer);
            return ResponseEntity.ok(Map.of(
                "message", "Tạo booking thành công. Vui lòng thanh toán cọc trong 15 phút.",
                "rentalId", rental.getId()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/customer/booking/payment/vnpay")
    public ResponseEntity<?> redirectToVnpay(@RequestBody Map<String, String> payload) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String rentalId = payload.get("rentalId");
        rentalOperationService.expirePendingBookingsWithoutDeposit();
        Rental rental = rentalRepository.findById(rentalId).orElse(null);

        if (rental == null || rental.getStatus() != RentalStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Booking không hợp lệ để thanh toán."));
        }
        if (!vnpayService.isReady()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cổng VNPay đang bảo trì."));
        }

        List<RentalDetail> details = rental.getRentalDetails();
        int stayHours = details.get(0).getLengthOfStay() == null ? minimumStayHours : Math.max(details.get(0).getLengthOfStay(), minimumStayHours);
        RentalType rentalType = details.get(0).getRentalType() != null ? details.get(0).getRentalType() : RentalType.HOURLY;

        double totalDepositAmount = 0;

        for (RentalDetail detail : details) {
            double unitPrice = detail.getUnitPrice() == null ? 0.0 : detail.getUnitPrice();
            LocalDateTime ci = detail.getCheckinDate() != null ? detail.getCheckinDate() : LocalDateTime.now();
            LocalDateTime co = ci.plusHours(stayHours);

            double subTotal = customerHelperService.calculateRoomSubTotal(unitPrice, ci, co, rentalType);
            totalDepositAmount += customerHelperService.calculateDepositAmount(subTotal, customerHelperService.resolveDepositPercent(detail.getRoom()));
        }

        try {
            String paymentUrl = vnpayService.buildPaymentUrl(
                    com.hotel.system.dto.vnpay.VnpayCreatePaymentRequest.builder()
                            .txnRef(rental.getId())
                            .rentalId(rental.getId())
                            .billType(BillType.DEPOSIT)
                            .amount((long) Math.round(totalDepositAmount))
                            .orderInfo("Thanh toan coc booking " + rental.getId())
                            .returnUrl("http://localhost:8080/api/public/vnpay-return")
                            .ipAddress("127.0.0.1")
                            .createdAt(LocalDateTime.now())
                            .expiredAt(rental.getRentDate() == null ? LocalDateTime.now().plusMinutes(10) : rental.getRentDate().plusMinutes(15))
                            .build()
            );
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi tạo thanh toán: " + ex.getMessage()));
        }
    }

    @GetMapping("/api/public/vnpay-return")
    public void handleVnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String frontendRedirectUrl = this.frontendReturnUrl;

        VnpayPaymentResult paymentResult = vnpayService.parsePaymentResult(params);
        String rentalId = paymentResult.getTxnRef();

        if (rentalId == null || rentalId.isBlank()) {
            response.sendRedirect(frontendRedirectUrl + "?error=invalid_txn");
            return;
        }

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || !paymentResult.isSuccess()) {
            response.sendRedirect(frontendRedirectUrl + "?error=payment_failed");
            return;
        }

        if (billRepository.existsByRentalIdAndType(rentalId, BillType.DEPOSIT)) {
            response.sendRedirect(frontendRedirectUrl + "?status=success");
            return;
        }

        List<RentalDetail> details = rental.getRentalDetails();
        int stayHours = details.get(0).getLengthOfStay() == null ? minimumStayHours : Math.max(details.get(0).getLengthOfStay(), minimumStayHours);
        RentalType rentalType = details.get(0).getRentalType() != null ? details.get(0).getRentalType() : RentalType.HOURLY;

        double totalDepositAmount = 0;

        for (RentalDetail detail : details) {
            double unitPrice = detail.getUnitPrice() == null ? 0.0 : detail.getUnitPrice();
            LocalDateTime ci = detail.getCheckinDate() != null ? detail.getCheckinDate() : LocalDateTime.now();
            LocalDateTime co = ci.plusHours(stayHours);

            double subTotal = customerHelperService.calculateRoomSubTotal(unitPrice, ci, co, rentalType);
            totalDepositAmount += customerHelperService.calculateDepositAmount(subTotal, customerHelperService.resolveDepositPercent(detail.getRoom()));
        }

        Bill depositBill = new Bill();
        depositBill.setId(AppUtils.generateId("BIL", 10));
        depositBill.setCreateDate(LocalDateTime.now());
        depositBill.setTotalAmount(totalDepositAmount);
        depositBill.setType(BillType.DEPOSIT);
        depositBill.setRental(rental);
        billRepository.save(depositBill);

        Payment payment = new Payment();
        payment.setId(AppUtils.generateId("PAY", 10));
        payment.setMethod(PaymentMethod.BANK);
        payment.setDate(LocalDateTime.now());
        payment.setTransaction(paymentResult.getTransactionNo() != null ? paymentResult.getTransactionNo() : "VNPAY-" + rentalId);
        payment.setBill(depositBill);
        paymentRepository.save(payment);

        rental.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental);

        if (rental.getCustomer() != null && rental.getCustomer().getUser() != null) {
            Users u = rental.getCustomer().getUser();
            String roomNames = details.stream()
                    .map(d -> d.getRoom() != null ? d.getRoom().getName() : "N/A")
                    .collect(Collectors.joining(", "));
            String checkInStr = details.get(0).getCheckinDate() != null
                    ? details.get(0).getCheckinDate().format(DATE_TIME_FORMATTER) : "N/A";
            emailService.sendBookingConfirmedEmail(
                    u.getEmail(),
                    AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                    rental.getId(),
                    roomNames,
                    checkInStr,
                    AppUtils.formatMoney(totalDepositAmount),
                    "Chuyển khoản VNPay"
            );
        }

        response.sendRedirect(frontendRedirectUrl + "?status=success");
    }

    @PostMapping("/api/customer/rentals/cancel")
    public ResponseEntity<?> cancelOwnBooking(@RequestBody Map<String, String> payload) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            String rentalId = payload.get("rentalId");
            Rental rental = rentalRepository.findById(rentalId).orElse(null);
            String roomNames = (rental != null && rental.getRentalDetails() != null)
                    ? rental.getRentalDetails().stream()
                            .map(d -> d.getRoom() != null ? d.getRoom().getName() : "N/A")
                            .collect(Collectors.joining(", "))
                    : "N/A";

            double depositPaid = customerBookingService.cancelBooking(rentalId, currentUser.getId());

            String cancelNote = depositPaid > 0
                    ? "Bạn đã chủ động hủy đơn đặt phòng. Tiền cọc không hoàn lại: " + AppUtils.formatMoney(depositPaid)
                    : "Bạn đã chủ động hủy đơn đặt phòng khi chưa thanh toán cọc.";
            emailService.sendBookingCancelledEmail(
                    currentUser.getEmail(),
                    AppUtils.buildFullName(currentUser.getFirstName(), currentUser.getLastName()),
                    rentalId,
                    roomNames,
                    cancelNote
            );

            if (depositPaid > 0) {
                return ResponseEntity.ok(Map.of("message", "Đã hủy booking. Mất cọc: " + AppUtils.formatMoney(depositPaid)));
            }
            return ResponseEntity.ok(Map.of("message", "Hủy booking thành công."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/customer/profile")
    public ResponseEntity<?> profile() {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        rentalOperationService.expirePendingBookingsWithoutDeposit();
        rentalOperationService.markCheckedInRentalsAsOverdue();

        Customer customerDetail = customerRepository.findById(currentUser.getId()).orElse(null);
        customerTierService.syncCustomerTierByBills(customerDetail);

        TierHistory currentTierHistory = tierHistoryRepository.findTopByCustomerIdAndEndDateIsNullOrderByStartDateDesc(currentUser.getId()).orElse(null);

        List<Rental> rentals = rentalRepository.findByCustomerId(currentUser.getId()).stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList();

        Set<String> rentalIds = rentals.stream().map(Rental::getId).filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet());
        List<Bill> bills = billRepository.findAllByOrderByCreateDateDesc().stream()
                .filter(bill -> bill.getRental() != null && rentalIds.contains(bill.getRental().getId())).toList();

        Set<String> billIds = bills.stream().map(Bill::getId).filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet());
        List<Payment> payments = paymentRepository.findAllByOrderByDateDesc().stream()
                .filter(payment -> payment.getBill() != null && billIds.contains(payment.getBill().getId())).toList();

        return ResponseEntity.ok(Map.of(
            "currentUser", currentUser,
            "currentTier", currentTierHistory != null ? currentTierHistory.getTierCustomer() : "Thành viên mới",
            "rentals", rentals,
            "bills", bills,
            "payments", payments
        ));
    }

    @PutMapping("/api/customer/profile/update")
    public ResponseEntity<?> updateProfile(@Valid @ModelAttribute ProfileUpdateRequest request, BindingResult bindingResult) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            Users updatedUser = profileService.updateProfile(currentUser, request);
            return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ thành công.", "user", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/customer/profile/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, BindingResult bindingResult) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            profileService.changePassword(currentUser, request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/customer/profile/account-statuses")
    public ResponseEntity<?> getCustomerAccountStatuses() {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<AccountStatus> statuses = accountStatusRepository
                .findByAccount_User_IdOrderByStartTimeDesc(currentUser.getId());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/api/customer/feedback/my")
    public ResponseEntity<?> getMyFeedback() {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Review myReview = reviewRepository.findByCustomerId(currentUser.getId()).orElse(null);
        return ResponseEntity.ok(Map.of("myReview", myReview != null ? myReview : ""));
    }

    @PostMapping("/api/customer/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> payload) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (reviewRepository.existsByCustomerId(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã đánh giá rồi."));
        }

        Review review = new Review();
        review.setId(AppUtils.generateId("REV", 10));
        review.setRate((Integer) payload.get("rate"));
        review.setDescription(AppUtils.safeTrim((String) payload.get("description")));
        review.setUpdateDate(LocalDateTime.now());
        review.setCustomer(customerRepository.findById(currentUser.getId()).orElseThrow());
        reviewRepository.save(review);

        return ResponseEntity.ok(Map.of("message", "Cám ơn đánh giá của bạn."));
    }

    @PutMapping("/api/customer/feedback")
    public ResponseEntity<?> updateFeedback(@RequestBody Map<String, Object> payload) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Review existingReview = reviewRepository.findByCustomerId(currentUser.getId()).orElse(null);
        if (existingReview == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy đánh giá."));

        existingReview.setRate((Integer) payload.get("rate"));
        existingReview.setDescription(AppUtils.safeTrim((String) payload.get("description")));
        existingReview.setUpdateDate(LocalDateTime.now());
        reviewRepository.save(existingReview);

        return ResponseEntity.ok(Map.of("message", "Cập nhật đánh giá thành công."));
    }

    @DeleteMapping("/api/customer/feedback")
    public ResponseEntity<?> deleteFeedback() {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        reviewRepository.findByCustomerId(currentUser.getId()).ifPresent(reviewRepository::delete);
        return ResponseEntity.ok(Map.of("message", "Đã xóa đánh giá."));
    }

    @PostMapping("/api/customer/profile/sync-tier")
    public ResponseEntity<?> syncMyTier() {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Customer customer = customerRepository.findById(currentUser.getId()).orElse(null);
        if (customer != null) {
            customerTierService.syncCustomerTierByBills(customer);
        }
        return ResponseEntity.ok(Map.of("message", "Dữ liệu hạng thẻ của bạn đã được làm mới!"));
    }
}