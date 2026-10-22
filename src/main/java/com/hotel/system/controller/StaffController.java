package com.hotel.system.controller;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

import com.hotel.system.dto.billing.BillingSummary;
import com.hotel.system.dto.timeline.RoomTimelineRowView;
import com.hotel.system.dto.timeline.TimelineSlotView;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.service.BillingService;
import com.hotel.system.service.CustomerTierService;
import com.hotel.system.service.RentalOperationService;
import com.hotel.system.service.RoomTimelineService;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.PasswordUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final int MINIMUM_WALK_IN_HOURS = 1;

    private final RoomTimelineService roomTimelineService;
    private final BillingService billingService;
    private final CustomerTierService customerTierService;
    private final RentalOperationService rentalOperationService;
    private final StaffRepository staffRepository;
    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final UsersRepository usersRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountStatusRepository accountStatusRepository;

    public StaffController(BillingService billingService,
                           RoomTimelineService roomTimelineService,
                           CustomerTierService customerTierService,
                           RentalOperationService rentalOperationService,
                           StaffRepository staffRepository,
                           RentalRepository rentalRepository,
                           RentalDetailRepository rentalDetailRepository,
                           RoomRepository roomRepository,
                           ServiceRepository serviceRepository,
                           ServiceUsageRepository serviceUsageRepository,
                           BillRepository billRepository,
                           PaymentRepository paymentRepository,
                           UsersRepository usersRepository,
                           CustomerRepository customerRepository,
                           AccountRepository accountRepository,
                           AccountStatusRepository accountStatusRepository) {
        this.billingService = billingService;
        this.roomTimelineService = roomTimelineService;
        this.customerTierService = customerTierService;
        this.rentalOperationService = rentalOperationService;
        this.staffRepository = staffRepository;
        this.rentalRepository = rentalRepository;
        this.rentalDetailRepository = rentalDetailRepository;
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
        this.serviceUsageRepository = serviceUsageRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.usersRepository = usersRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountStatusRepository = accountStatusRepository;
    }

    private Users getLoggedInStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent() && (userOpt.get().getRole() == Role.STAFF || userOpt.get().getRole() == Role.MANAGER || userOpt.get().getRole() == Role.ADMIN)) {
                return userOpt.get();
            }
        }
        return null;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> staffDashboard(@RequestParam(name = "timelineOffsetHours", defaultValue = "0") Integer timelineOffsetHours,
                                 @RequestParam(name = "walkInCheckin", required = false) String walkInCheckin,
                                 @RequestParam(name = "walkInLengthOfStay", defaultValue = "1") Integer walkInLengthOfStay) {
        Users currentUser = getLoggedInStaff();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Access Denied"));

        rentalOperationService.expirePendingBookingsWithoutDeposit();
        rentalOperationService.markCheckedInRentalsAsOverdue();
        rentalOperationService.autoReleaseCleaningRooms();

        List<Rental> rentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Rental> pendingBookings = rentals.stream()
                .filter(rental -> Boolean.TRUE.equals(rental.getIsBooking()))
                .filter(rental -> rental.getStatus() == RentalStatus.PENDING)
                .toList();

        List<Rental> confirmedBookings = rentals.stream()
                .filter(rental -> Boolean.TRUE.equals(rental.getIsBooking()))
                .filter(rental -> rental.getStatus() == RentalStatus.CONFIRMED)
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .toList();

        List<Rental> checkedInRentals = rentals.stream()
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN))
                .toList();

        List<Rental> overdueRentals = rentals.stream()
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.OVERDUE))
                .toList();

        List<Rental> checkedOutRentals = rentals.stream()
                .filter(rental -> rental.getStatus() == RentalStatus.COMPLETED)
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getRoom() != null && d.getRoom().getStatus() == RoomStatus.CLEANING))
                .toList();

        List<Rental> todayArrivals = confirmedBookings.stream()
                .filter(rental -> rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty()
                                   && rental.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(rental -> rental.getRentalDetails().get(0).getCheckinDate().toLocalDate().isEqual(LocalDate.now()))
                .toList();

        List<Service> activeServices = serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE);
        List<ServiceUsage> serviceUsages = serviceUsageRepository.findAllByOrderByTimeDesc();
        List<Bill> bills = billRepository.findAllByOrderByCreateDateDesc();
        List<Payment> payments = paymentRepository.findAllByOrderByDateDesc();

        List<Room> allRooms = roomRepository.findAll().stream()
                .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        int safeTimelineOffsetHours = normalizeTimelineOffsetHours(timelineOffsetHours);
        LocalDateTime baseTimelineStart = floorToHalfHour(LocalDateTime.now());
        LocalDateTime timelineStart = baseTimelineStart.plusHours(safeTimelineOffsetHours);
        LocalDateTime timelineEnd = timelineStart.plusHours(24);

        int safeWalkInLengthOfStay = walkInLengthOfStay == null || walkInLengthOfStay < MINIMUM_WALK_IN_HOURS ? MINIMUM_WALK_IN_HOURS : walkInLengthOfStay;
        LocalDateTime parsedStart = parseDashboardDateTimeOrDefault(walkInCheckin, baseTimelineStart);
        LocalDateTime walkInSearchStart = parsedStart.isBefore(baseTimelineStart) ? baseTimelineStart : parsedStart;
        LocalDateTime walkInSearchEnd = walkInSearchStart.plusHours(safeWalkInLengthOfStay).plusMinutes(CLEANING_BUFFER_MINUTES);

        List<Room> walkInRooms = allRooms.stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> !rentalOperationService.hasBlockingConflict(room.getId(), walkInSearchStart, walkInSearchEnd, null))
                .toList();

        List<TimelineSlotView> roomTimelineSlots = roomTimelineService.buildRoomTimelineSlots(timelineStart, 48);
        List<RoomTimelineRowView> roomTimelineRows = roomTimelineService.buildRoomTimelineRows(allRooms, rentals, timelineStart, timelineEnd);

        Map<String, Object> response = new HashMap<>();
        response.put("pendingBookings", pendingBookings);
        response.put("confirmedBookings", confirmedBookings);
        response.put("checkedInRentals", checkedInRentals);
        response.put("overdueRentals", overdueRentals);
        response.put("checkedOutRentals", checkedOutRentals);
        response.put("todayArrivals", todayArrivals);
        response.put("activeServices", activeServices);
        response.put("serviceUsages", serviceUsages);
        response.put("bills", bills);
        response.put("payments", payments);
        
        response.put("walkInRooms", walkInRooms);
        response.put("roomTimelineSlots", roomTimelineSlots);
        response.put("roomTimelineRows", roomTimelineRows);
        response.put("timelineStart", timelineStart);
        response.put("timelineEnd", timelineEnd);

        response.put("availableNowRoomCount", roomTimelineRows.stream().filter(RoomTimelineRowView::availableNow).count());
        response.put("busyNowRoomCount", roomTimelineRows.stream().filter(row -> !row.availableNow() && !row.maintenanceRoom()).count());
        response.put("maintenanceRoomCount", roomTimelineRows.stream().filter(RoomTimelineRowView::maintenanceRoom).count());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/walk-in")
    @Transactional
    public ResponseEntity<?> createWalkInRental(@RequestBody Map<String, Object> payload) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            List<String> roomIds = (List<String>) payload.get("roomId");
            List<Integer> adultCounts = (List<Integer>) payload.get("adultCount");
            List<Integer> childrenCounts = (List<Integer>) payload.get("childrenCount");
            Integer lengthOfStay = Integer.valueOf(payload.get("lengthOfStay").toString());
            
            if (roomIds == null || roomIds.isEmpty()) throw new RuntimeException("Vui lòng chọn ít nhất 1 phòng.");
            
            String existingCustomerPid = (String) payload.get("existingCustomerPid");
            Customer customer = null;
            
            if (existingCustomerPid != null && !existingCustomerPid.trim().isEmpty()) {
                Users user = usersRepository.findByPid(existingCustomerPid.trim())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + existingCustomerPid));
                customer = customerRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Tài khoản này không thuộc nhóm Khách Hàng."));
            } else {
                String pid = (String) payload.get("pid");
                if (pid == null || pid.trim().isEmpty()) throw new RuntimeException("Vui lòng nhập CCCD.");
                if (usersRepository.findByPid(pid.trim()).isPresent()) {
                    throw new RuntimeException("CCCD/Hộ chiếu này đã có trên hệ thống! Vui lòng nhập vào ô dành cho Khách Cũ.");
                }

                Users newUser = new Users();
                newUser.setLastName((String) payload.get("lastName"));
                newUser.setFirstName((String) payload.get("firstName"));
                newUser.setEmail((String) payload.get("email"));
                newUser.setPid(pid);
                newUser.setPhoneNumber((String) payload.get("phoneNumber"));
                newUser.setNationality((String) payload.get("nationality"));
                if (payload.get("sex") != null) newUser.setSex(Gender.valueOf(payload.get("sex").toString()));
                if (payload.get("dateOfBirth") != null) newUser.setDateOfBirth(LocalDate.parse(payload.get("dateOfBirth").toString()));
                newUser.setRole(Role.CUSTOMER);
                newUser.setCreateDate(LocalDateTime.now());
                newUser.setUpdateDate(LocalDateTime.now());
                newUser = usersRepository.save(newUser);

                customer = new Customer();
                customer.setUser(newUser);
                customer = customerRepository.save(customer);

                Account acc = new Account();
                acc.setUser(newUser);
                acc.setPassword(PasswordUtils.hashPassword("123456"));
                accountRepository.save(acc);

                AccountStatus accStatus = new AccountStatus();
                accStatus.setAccount(acc);
                accStatus.setName(AccountState.ACTIVE);
                accStatus.setStartTime(LocalDateTime.now());
                accStatus.setReason("Tạo tài khoản khi khách Walk-in");
                accountStatusRepository.save(accStatus);
            }

            Rental rental = new Rental();
            rental.setId(AppUtils.generateId("REN", 10));
            rental.setRentDate(LocalDateTime.now());
            rental.setIsBooking(false);
            rental.setStatus(RentalStatus.CONFIRMED); 
            rental.setCustomer(customer);
            rental = rentalRepository.save(rental);

            for (int i = 0; i < roomIds.size(); i++) {
                Room room = roomRepository.findById(roomIds.get(i)).orElseThrow();
                RentalDetail detail = new RentalDetail();
                detail.setId(AppUtils.generateId("RDT", 10));
                detail.setRental(rental);
                detail.setRoom(room);
                detail.setCheckinDate(LocalDateTime.now());
                detail.setLengthOfStay(lengthOfStay);
                detail.setAdultCount(adultCounts.get(i));
                detail.setChildrenCount(childrenCounts.get(i));
                detail.setUnitPrice(room.getRoomType().getBasePrice());
                detail.setStatus(RentalDetailStatus.CHECKED_IN); 
                rentalDetailRepository.save(detail);
            }
            return ResponseEntity.ok(Map.of("message", "Tạo đơn thuê thành công cho " + roomIds.size() + " phòng!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo đơn thuê: " + e.getMessage()));
        }
    }

    @PostMapping("/bookings/{rentalId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable String rentalId) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || !Boolean.TRUE.equals(rental.getIsBooking())) return ResponseEntity.badRequest().body(Map.of("error", "Không thể huỷ."));

        rental.setStatus(RentalStatus.CANCELLED);
        for(RentalDetail detail : rental.getRentalDetails()){
            detail.setStatus(RentalDetailStatus.CANCELLED);
        }
        rentalRepository.save(rental);
        return ResponseEntity.ok(Map.of("message", "Đã hủy booking thành công."));
    }

    @PostMapping("/rentals/{rentalId}/check-in")
    @Transactional
    public ResponseEntity<?> checkIn(@PathVariable String rentalId) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ được check-in booking đã xác nhận."));
        }

        for (RentalDetail detail : rental.getRentalDetails()) {
            if (detail.getRoom() == null || detail.getRoom().getStatus() != RoomStatus.AVAILABLE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phòng " + (detail.getRoom() != null ? detail.getRoom().getName() : "") + " hiện chưa trống."));
            }
        }

        for (RentalDetail detail : rental.getRentalDetails()) {
            detail.setStatus(RentalDetailStatus.CHECKED_IN);
            rentalDetailRepository.save(detail);
        }
        return ResponseEntity.ok(Map.of("message", "Check-in thành công cho tất cả các phòng."));
    }

    @PostMapping("/rentals/{rentalId}/services")
    @Transactional
    public ResponseEntity<?> addServiceToRental(@PathVariable String rentalId, @RequestBody Map<String, Object> payload) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy Rental."));

        boolean isActive = rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE);
        if (!isActive) return ResponseEntity.badRequest().body(Map.of("error", "Chỉ thêm dịch vụ cho khách đang ở."));

        Service service = serviceRepository.findById(payload.get("serviceId").toString()).orElse(null);
        if (service == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy dịch vụ."));

        RentalDetail firstDetail = rental.getRentalDetails().get(0);

        ServiceUsage usage = new ServiceUsage();
        usage.setId(AppUtils.generateId("SVG", 10));
        usage.setCount(Integer.valueOf(payload.get("quantity").toString()));
        usage.setTime(LocalDateTime.now());
        usage.setUnitPrice(service.getBasePrice());
        usage.setRentalDetail(firstDetail);
        usage.setService(service);
        serviceUsageRepository.save(usage);

        return ResponseEntity.ok(Map.of("message", "Thêm dịch vụ thành công."));
    }

    @PostMapping("/rentals/{rentalId}/final-payment")
    @Transactional
    public ResponseEntity<?> collectFinalPayment(@PathVariable String rentalId, @RequestBody Map<String, String> payload) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy Rental."));

        boolean isActive = rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE);
        if (!isActive) return ResponseEntity.badRequest().body(Map.of("error", "Chỉ ghi nhận thanh toán cuối cho khách đang ở."));

        PaymentMethod paymentMethod = PaymentMethod.valueOf(payload.get("paymentMethod"));
        BillingSummary billingSummary = billingService.buildBillingSummary(rental);
        
        Bill finalBill = new Bill();
        finalBill.setId(AppUtils.generateId("BIL", 10));
        finalBill.setCreateDate(LocalDateTime.now());
        finalBill.setTotalAmount(billingSummary.finalAmount());
        finalBill.setType(billingSummary.billType());
        finalBill.setActualStayHours(billingSummary.actualStayHours());
        finalBill.setActualRoomAmount(billingSummary.roomAmount());
        finalBill.setEarlyCheckoutPenaltyPercent(billingSummary.earlyCheckoutPenaltyPercent());
        finalBill.setRental(rental);
        billRepository.save(finalBill);

        Payment payment = new Payment();
        payment.setId(AppUtils.generateId("PAY", 10));
        payment.setMethod(paymentMethod);
        payment.setDate(LocalDateTime.now());
        payment.setTransaction(AppUtils.buildTransactionCode(paymentMethod, payload.get("transactionCode")));
        payment.setBill(finalBill);
        paymentRepository.save(payment);

        customerTierService.syncCustomerTierByBills(rental.getCustomer());
        return ResponseEntity.ok(Map.of("message", "Đã ghi nhận thanh toán cuối."));
    }

    @PostMapping("/rentals/{rentalId}/check-out")
    @Transactional
    public ResponseEntity<?> checkOut(@PathVariable String rentalId) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null) return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy Rental."));

        if (!billingService.hasSettlementPayment(rentalId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chưa thể check-out. Cần thanh toán cuối trước."));
        }

        rental.setStatus(RentalStatus.COMPLETED);
        rentalRepository.save(rental);

        for (RentalDetail detail : rental.getRentalDetails()) {
            if (detail.getRoom() != null) {
                Room room = detail.getRoom();
                room.setStatus(RoomStatus.CLEANING);
                roomRepository.save(room);
            }
            detail.setStatus(RentalDetailStatus.CHECKED_OUT);
            rentalDetailRepository.save(detail);
        }

        return ResponseEntity.ok(Map.of("message", "Hoàn tất check-out. Các phòng chuyển sang trạng thái đang dọn dẹp."));
    }

    @PutMapping("/rooms/{roomId}/status/available")
    @Transactional
    public ResponseEntity<?> markRoomAvailable(@PathVariable String roomId) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null && room.getStatus() == RoomStatus.CLEANING) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
            return ResponseEntity.ok(Map.of("message", "Đã mở lại phòng " + room.getName()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Phòng không ở trạng thái cần dọn dẹp."));
    }

    private int normalizeTimelineOffsetHours(Integer timelineOffsetHours) {
        if (timelineOffsetHours == null) return 0;
        int remainder = timelineOffsetHours % 24;
        return remainder == 0 ? timelineOffsetHours : timelineOffsetHours - remainder;
    }

    private LocalDateTime parseDashboardDateTimeOrDefault(String value, LocalDateTime defaultValue) {
        try {
            return (value == null || value.isBlank()) ? defaultValue : LocalDateTime.parse(value.trim());
        } catch (Exception ex) { return defaultValue; }
    }

    private String formatForDateTimeLocal(LocalDateTime value) {
        return value == null ? "" : value.withSecond(0).withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private LocalDateTime floorToHalfHour(LocalDateTime value) {
        if (value == null) return null;
        int flooredMinute = value.getMinute() < 30 ? 0 : 30;
        return value.withMinute(flooredMinute).withSecond(0).withNano(0);
    }
}