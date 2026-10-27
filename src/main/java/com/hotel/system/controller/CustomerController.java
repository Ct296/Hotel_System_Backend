package com.hotel.system.controller;

import com.hotel.system.dto.customer.BookingConfirmRequest;
import com.hotel.system.dto.profile.ChangePasswordRequest;
import com.hotel.system.dto.profile.ProfileUpdateRequest;
import com.hotel.system.dto.vnpay.VnpayPaymentResult;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.service.*;
import com.hotel.system.util.AppUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class CustomerController {

    private static final Set<RentalDetailStatus> BLOCKING_BOOKING_STATUSES = EnumSet.of(
            RentalDetailStatus.PENDING, RentalDetailStatus.CHECKED_IN, RentalDetailStatus.OVERDUE
    );

    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final long BOOKING_HOLD_MINUTES = 30;
    private static final int MINIMUM_STAY_HOURS = 1;
    private static final int SHORT_STAY_SURCHARGE_THRESHOLD_HOURS = 6;
    private static final double SHORT_STAY_SURCHARGE_PERCENT = 10.0;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final TierHistoryRepository tierHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;
    private final AppliedPeriodRepository appliedPeriodRepository;
    private final UsersRepository usersRepository;

    private final VnpayService vnpayService;
    private final CustomerBookingService customerBookingService;
    private final ProfileService profileService;
    private final RentalOperationService rentalOperationService;
    private final CustomerTierService customerTierService;

    // DTOs & Records
    private record RoomCardView(Room room, double displayPrice, String roomTypeName, double basePrice) {}

    public CustomerController(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository,
                              RoomImageRepository roomImageRepository, CustomerRepository customerRepository,
                              RentalRepository rentalRepository, TierHistoryRepository tierHistoryRepository,
                              ReviewRepository reviewRepository, BillRepository billRepository,
                              PaymentRepository paymentRepository, ServiceRepository serviceRepository,
                              AppliedPeriodRepository appliedPeriodRepository, UsersRepository usersRepository,
                              VnpayService vnpayService, CustomerBookingService customerBookingService,
                              ProfileService profileService, RentalOperationService rentalOperationService,
                              CustomerTierService customerTierService) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomImageRepository = roomImageRepository;
        this.customerRepository = customerRepository;
        this.rentalRepository = rentalRepository;
        this.tierHistoryRepository = tierHistoryRepository;
        this.reviewRepository = reviewRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRepository = serviceRepository;
        this.appliedPeriodRepository = appliedPeriodRepository;
        this.usersRepository = usersRepository;
        this.vnpayService = vnpayService;
        this.customerBookingService = customerBookingService;
        this.profileService = profileService;
        this.rentalOperationService = rentalOperationService;
        this.customerTierService = customerTierService;
    }

    private Users getLoggedInCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.CUSTOMER) {
                return userOpt.get();
            }
        }
        return null;
    }

    // ==========================================
    // CÁC API PUBLIC (KHÔNG CẦN ĐĂNG NHẬP)
    // ==========================================

    @GetMapping("/api/public/home")
    public ResponseEntity<?> home() {
        List<RoomType> roomTypes = roomTypeRepository.findAll().stream().sorted(Comparator.comparing(RoomType::getName)).toList();
        List<RoomType> roomTypesByPriceDesc = roomTypes.stream().sorted(Comparator.comparing(this::getSafeBasePrice).reversed()
                        .thenComparing(RoomType::getName, Comparator.nullsLast(String::compareToIgnoreCase))).toList();
        List<RoomType> roomTypesByPriceAsc = roomTypes.stream().sorted(Comparator.comparing(this::getSafeBasePrice)
                        .thenComparing(RoomType::getName, Comparator.nullsLast(String::compareToIgnoreCase))).toList();

        List<RoomType> luxuryRoomTypes = roomTypesByPriceDesc.stream().limit(3).toList();
        Set<String> luxuryRoomTypeIds = luxuryRoomTypes.stream().map(RoomType::getId).filter(StringUtils::hasText).collect(Collectors.toSet());
        List<RoomType> budgetRoomTypes = roomTypesByPriceAsc.stream().filter(type -> !luxuryRoomTypeIds.contains(type.getId())).limit(3).toList();

        return ResponseEntity.ok(Map.of(
            "roomTypes", roomTypes,
            "luxuryRoomTypes", luxuryRoomTypes,
            "budgetRoomTypes", budgetRoomTypes
        ));
    }

    @GetMapping("/api/public/rooms")
    public ResponseEntity<?> rooms(@RequestParam(required = false) String typeId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
                                   @RequestParam(required = false, defaultValue = "default") String sortBy) {
        rentalOperationService.expirePendingBookingsWithoutDeposit();

        LocalDateTime minimumBookingStartTime = getMinimumBookingStartTime();
        LocalDateTime adjustedCheckIn = checkIn != null ? checkIn : minimumBookingStartTime;
        LocalDateTime adjustedCheckOut = checkOut != null ? checkOut : adjustedCheckIn.plusHours(1);

        if (adjustedCheckIn.isBefore(minimumBookingStartTime)) adjustedCheckIn = minimumBookingStartTime;
        if (!adjustedCheckOut.isAfter(adjustedCheckIn)) adjustedCheckOut = adjustedCheckIn.plusHours(1);

        final LocalDateTime searchCheckIn = adjustedCheckIn;
        final LocalDateTime searchCheckOut = adjustedCheckOut;

        List<RoomCardView> roomCards = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> typeId == null || typeId.isBlank() || (room.getRoomType() != null && typeId.equals(room.getRoomType().getId())))
                .filter(room -> isRoomAvailable(room, searchCheckIn, searchCheckOut))
                .map(room -> {
                    List<String> images = getRoomGalleryPaths(room);
                    return new RoomCardView(room, resolveRoomUnitPrice(room, searchCheckIn),
                        room.getRoomType() != null ? room.getRoomType().getName() : "",
                        room.getRoomType() != null && room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() : 0.0);
                })
                .sorted(resolveRoomSort(sortBy)).toList();

        List<Map<String, Object>> responseRooms = new ArrayList<>();
        for (RoomCardView view : roomCards) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", view.room().getId());
            r.put("name", view.room().getName());
            r.put("roomType", view.room().getRoomType());
            r.put("displayPrice", view.displayPrice());
            r.put("images", getRoomGalleryPaths(view.room()));
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

    // ==========================================
    // CÁC API PROTECTED (CHỈ DÀNH CHO CUSTOMER)
    // ==========================================

    @PostMapping("/api/customer/booking/calculate")
    public ResponseEntity<?> calculateBooking(@RequestBody Map<String, Object> payload) {
        List<String> roomIds = (List<String>) payload.get("roomId");
        if (roomIds == null || roomIds.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn ít nhất 1 phòng."));

        LocalDateTime checkIn = payload.get("checkIn") != null ? LocalDateTime.parse(payload.get("checkIn").toString()) : null;
        LocalDateTime checkOut = payload.get("checkOut") != null ? LocalDateTime.parse(payload.get("checkOut").toString()) : null;

        List<Room> selectedRooms = roomRepository.findAllById(roomIds);
        if (selectedRooms.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy phòng."));

        LocalDateTime minimumBookingStartTime = getMinimumBookingStartTime();
        LocalDateTime finalCheckIn = checkIn != null ? checkIn : minimumBookingStartTime;
        LocalDateTime finalCheckOut = checkOut != null ? checkOut : finalCheckIn.plusHours(1);

        if (finalCheckIn.isBefore(minimumBookingStartTime)) finalCheckIn = minimumBookingStartTime;
        if (!finalCheckOut.isAfter(finalCheckIn)) finalCheckOut = finalCheckIn.plusHours(1);

        double totalRoomUnitPrice = 0;
        boolean bookingAvailable = true;
        String conflictMessage = null;

        for (Room room : selectedRooms) {
            totalRoomUnitPrice += resolveRoomUnitPrice(room, finalCheckIn);
            Rental conflictRental = findBlockingRentalConflict(room, finalCheckIn, finalCheckOut);
            if (conflictRental != null && bookingAvailable) {
                bookingAvailable = false;
                conflictMessage = "Phòng " + room.getName() + " đang bị trùng lịch trong khung: " + buildConflictRange(conflictRental);
            }
        }

        int stayHours = calculateStayHours(finalCheckIn, finalCheckOut);
        double roomSubTotal = calculateRoomSubTotal(totalRoomUnitPrice, stayHours);
        double shortStaySurchargeAmount = calculateShortStaySurcharge(roomSubTotal, stayHours);
        double totalPrice = roomSubTotal + shortStaySurchargeAmount;

        double totalDepositAmount = 0;
        for (Room room : selectedRooms) {
            double singleSub = calculateRoomSubTotal(resolveRoomUnitPrice(room, finalCheckIn), stayHours);
            double singleSurch = calculateShortStaySurcharge(singleSub, stayHours);
            totalDepositAmount += calculateDepositAmount(singleSub + singleSurch, resolveDepositPercent(room));
        }

        return ResponseEntity.ok(Map.of(
            "rooms", selectedRooms,
            "checkIn", finalCheckIn,
            "checkOut", finalCheckOut,
            "stayHours", stayHours,
            "totalPrice", totalPrice,
            "depositAmount", totalDepositAmount,
            "remainingAmount", totalPrice - totalDepositAmount,
            "bookingAvailable", bookingAvailable,
            "conflictMessage", conflictMessage != null ? conflictMessage : ""
        ));
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
                "message", "Tạo booking thành công. Vui lòng thanh toán cọc trong 30 phút.",
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
        int stayHours = details.get(0).getLengthOfStay() == null ? MINIMUM_STAY_HOURS : Math.max(details.get(0).getLengthOfStay(), MINIMUM_STAY_HOURS);
        double totalDepositAmount = 0;

        for (RentalDetail detail : details) {
            double subTotal = calculateRoomSubTotal(detail.getUnitPrice() == null ? 0.0 : detail.getUnitPrice(), stayHours);
            double surcharge = calculateShortStaySurcharge(subTotal, stayHours);
            totalDepositAmount += calculateDepositAmount(subTotal + surcharge, resolveDepositPercent(detail.getRoom()));
        }

        try {
            String paymentUrl = vnpayService.buildPaymentUrl(
                    com.hotel.system.dto.vnpay.VnpayCreatePaymentRequest.builder()
                            .txnRef(rental.getId())
                            .rentalId(rental.getId())
                            .billType(BillType.DEPOSIT)
                            .amount((long) Math.round(totalDepositAmount))
                            .orderInfo("Thanh toan coc booking " + rental.getId())
                            .returnUrl("http://localhost:8080/api/public/vnpay-return") // URL Nhận Callback
                            .ipAddress("127.0.0.1")
                            .createdAt(LocalDateTime.now())
                            .expiredAt(rental.getRentDate() == null ? LocalDateTime.now().plusMinutes(30) : rental.getRentDate().plusMinutes(30))
                            .build()
            );
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi tạo thanh toán: " + ex.getMessage()));
        }
    }

    // VNPay Callback URL - Public
    @GetMapping("/api/public/vnpay-return")
    public void handleVnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String frontendRedirectUrl = "http://localhost:5173/customer/profile"; // Thay bằng URL Vue thực tế

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
        int stayHours = details.get(0).getLengthOfStay() == null ? MINIMUM_STAY_HOURS : Math.max(details.get(0).getLengthOfStay(), MINIMUM_STAY_HOURS);
        double totalDepositAmount = 0;

        for (RentalDetail detail : details) {
            double subTotal = calculateRoomSubTotal(detail.getUnitPrice() == null ? 0.0 : detail.getUnitPrice(), stayHours);
            double surcharge = calculateShortStaySurcharge(subTotal, stayHours);
            totalDepositAmount += calculateDepositAmount(subTotal + surcharge, resolveDepositPercent(detail.getRoom()));
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

        response.sendRedirect(frontendRedirectUrl + "?status=success");
    }

    @PostMapping("/api/customer/rentals/cancel")
    public ResponseEntity<?> cancelOwnBooking(@RequestBody Map<String, String> payload) {
        Users currentUser = getLoggedInCustomer();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            double depositPaid = customerBookingService.cancelBooking(payload.get("rentalId"), currentUser.getId());
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

    // --- PRIVATE UTILS KHÔNG THAY ĐỔI ---
    private boolean isRoomAvailable(Room room, LocalDateTime checkIn, LocalDateTime checkOut) {
        return findBlockingRentalConflict(room, checkIn, checkOut) == null;
    }

    private Rental findBlockingRentalConflict(Room room, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (room == null || room.getStatus() != RoomStatus.AVAILABLE || checkIn == null || checkOut == null) return new Rental();

        LocalDateTime requestedStart = checkIn;
        LocalDateTime requestedEnd = checkOut.plusMinutes(CLEANING_BUFFER_MINUTES);

        return rentalRepository.findAll().stream()
                .filter(rental -> rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty() && rental.getRentalDetails().get(0).getRoom() != null)
                .filter(rental -> rental.getRentalDetails().get(0).getRoom().getId().equals(room.getId()))
                .filter(rental -> rental.getRentalDetails().get(0).getStatus() != null && BLOCKING_BOOKING_STATUSES.contains(rental.getRentalDetails().get(0).getStatus()))
                .filter(rental -> rental.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(rental -> rental.getRentalDetails().get(0).getLengthOfStay() != null && rental.getRentalDetails().get(0).getLengthOfStay() > 0)
                .filter(rental -> {
                    LocalDateTime exStart = rental.getRentalDetails().get(0).getCheckinDate();
                    LocalDateTime exEnd = rentalOperationService.calculateRentalEndWithBuffer(rental);
                    return exEnd != null && exStart.isBefore(requestedEnd) && exEnd.isAfter(requestedStart);
                }).min(Comparator.comparing(r -> r.getRentalDetails().get(0).getCheckinDate())).orElse(null);
    }

    private String buildConflictRange(Rental rental) {
        RentalDetail firstDetail = (rental != null && rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty()) ? rental.getRentalDetails().get(0) : null;
        if (firstDetail == null || firstDetail.getCheckinDate() == null || firstDetail.getLengthOfStay() == null) return "không xác định";
        LocalDateTime end = rentalOperationService.calculateRentalEndWithBuffer(rental);
        return end == null ? "không xác định" : firstDetail.getCheckinDate().format(DATE_TIME_FORMATTER) + " đến " + end.format(DATE_TIME_FORMATTER);
    }

    private int calculateStayHours(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) return MINIMUM_STAY_HOURS;
        long stayHours = ChronoUnit.HOURS.between(checkIn, checkOut);
        return stayHours < MINIMUM_STAY_HOURS ? MINIMUM_STAY_HOURS : (int) stayHours;
    }

    private double calculateRoomSubTotal(double roomUnitPrice, int stayHours) {
        return Math.max(roomUnitPrice, 0.0) * Math.max(stayHours, MINIMUM_STAY_HOURS);
    }

    private double calculateShortStaySurcharge(double roomSubTotal, int stayHours) {
        return stayHours < SHORT_STAY_SURCHARGE_THRESHOLD_HOURS ? roomSubTotal * SHORT_STAY_SURCHARGE_PERCENT / 100.0 : 0.0;
    }

    private double resolveDepositPercent(Room room) {
        if (room == null || room.getRoomType() == null || room.getRoomType().getDepositPercent() == null) return 0.0;
        double dp = room.getRoomType().getDepositPercent();
        return dp < 0 ? 0.0 : (dp > 100 ? 100.0 : dp);
    }

    private double calculateDepositAmount(double totalAmount, double depositPercent) {
        double dp = depositPercent < 0 ? 0.0 : (depositPercent > 100 ? 100.0 : depositPercent);
        return Math.max(totalAmount, 0.0) * dp / 100.0;
    }

    private double resolveRoomUnitPrice(Room room, LocalDateTime checkInDate) {
        if (room == null || room.getRoomType() == null) return 0.0;
        double basePrice = room.getRoomType().getBasePrice() == null ? 0.0 : room.getRoomType().getBasePrice();
        if (room.getRoomType().getId() == null || checkInDate == null) return basePrice;
        
        AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(room.getRoomType().getId())
                .stream().filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !checkInDate.isBefore(p.getStartDate()) && !checkInDate.isAfter(p.getEndDate()))
                .findFirst().orElse(null);
        if (matchedPeriod != null && matchedPeriod.getPriceRate() != null && matchedPeriod.getPriceRate().getSurchargeAmount() != null) {
            return basePrice + matchedPeriod.getPriceRate().getSurchargeAmount();
        }
        return basePrice;
    }

    private LocalDateTime getMinimumBookingStartTime() {
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        return LocalDateTime.now().isAfter(now) ? now.plusHours(1) : now;
    }

    private double getSafeBasePrice(RoomType roomType) { return roomType == null || roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice(); }

    private String normalizeRoomSort(String sortBy) {
        String normalized = AppUtils.safeTrim(sortBy).toLowerCase();
        return switch (normalized) {
            case "floor_desc", "floor_asc", "price_desc", "price_asc", "tier_desc", "tier_asc" -> normalized;
            default -> "default";
        };
    }

    private Comparator<RoomCardView> resolveRoomSort(String sortBy) {
        String normalizedSort = normalizeRoomSort(sortBy);
        Comparator<RoomCardView> defaultComparator = Comparator.comparing((RoomCardView item) -> AppUtils.safeTrim(item.room().getName()).replaceAll("\\d+", "").trim(), Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(this::extractFloorNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
                
        return switch (normalizedSort) {
            case "floor_desc" -> Comparator.comparing(this::extractFloorNumber, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "floor_asc" -> Comparator.comparing(this::extractFloorNumber, Comparator.nullsLast(Integer::compareTo)).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "price_desc" -> Comparator.comparing(RoomCardView::displayPrice, Comparator.reverseOrder()).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "price_asc" -> Comparator.comparing(RoomCardView::displayPrice).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "tier_desc" -> Comparator.comparing(RoomCardView::basePrice, Comparator.reverseOrder()).thenComparing(RoomCardView::roomTypeName, Comparator.nullsLast(String::compareToIgnoreCase)).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "tier_asc" -> Comparator.comparing(RoomCardView::basePrice).thenComparing(RoomCardView::roomTypeName, Comparator.nullsLast(String::compareToIgnoreCase)).thenComparing(item -> item.room().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            default -> defaultComparator;
        };
    }

    private Integer extractFloorNumber(RoomCardView item) {
        if (item == null || item.room() == null) return null;
        String digits = AppUtils.safeTrim(item.room().getName()).replaceAll("\\D+", "");
        if (digits.isBlank()) return null;
        try {
            return digits.length() >= 3 ? Integer.parseInt(digits.substring(0, digits.length() - 2)) : (digits.length() == 2 ? Integer.parseInt(digits.substring(0, 1)) : Integer.parseInt(digits));
        } catch (NumberFormatException ex) { return null; }
    }

    private List<String> getRoomGalleryPaths(Room room) {
        if (room == null) return List.of(getDefaultRoomImagePath());
        List<String> paths = roomImageRepository.findAllByRoomOrderByIsPrimaryDesc(room).stream()
                .map(RoomImage::getImagePath).filter(StringUtils::hasText).distinct().toList();
        return paths.isEmpty() ? List.of(getDefaultRoomImagePath()) : paths;
    }

    private String getDefaultRoomImagePath() {
        return "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=900&q=80";
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