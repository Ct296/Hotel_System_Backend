package com.hotel.system.controller;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

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

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        rentalOperationService.expirePendingBookingsWithoutDeposit();
        rentalOperationService.markCheckedInRentalsAsOverdue();
        rentalOperationService.autoReleaseCleaningRooms();

        List<Rental> rentals = rentalRepository.findAll();
        
        long pendingCount = rentals.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsBooking()) && r.getStatus() == RentalStatus.PENDING).count();
        
        List<Rental> todayArrivalsList = rentals.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsBooking()) && r.getStatus() == RentalStatus.CONFIRMED)
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .filter(r -> !r.getRentalDetails().isEmpty() && r.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(r -> r.getRentalDetails().get(0).getCheckinDate().toLocalDate().isEqual(LocalDate.now()))
                .sorted(Comparator.comparing(r -> r.getRentalDetails().get(0).getCheckinDate()))
                .toList();

        long checkedInCount = rentals.stream()
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE))
                .count();

        long cleaningCount = roomRepository.findByStatus(RoomStatus.CLEANING).size();
        long availableCount = roomRepository.findByStatus(RoomStatus.AVAILABLE).size();
        long maintenanceCount = roomRepository.findByStatus(RoomStatus.MAINTENANCE).size();
        long totalRooms = roomRepository.count();
        long busyCount = Math.max(0, totalRooms - availableCount - cleaningCount - maintenanceCount);

        return ResponseEntity.ok(Map.of(
                "pendingBookingCount", pendingCount,
                "todayArrivalsCount", todayArrivalsList.size(),
                "checkedInCount", checkedInCount,
                "checkedOutCount", cleaningCount,
                "todayArrivalsList", todayArrivalsList, 
                "roomStatusCounts", List.of(availableCount, busyCount, cleaningCount, maintenanceCount) 
        ));
    }

    @GetMapping("/timeline")
    public ResponseEntity<?> getTimeline(@RequestParam(name = "timelineOffsetHours", defaultValue = "0") Integer timelineOffsetHours) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Rental> rentals = rentalRepository.findAll();
        List<Room> allRooms = roomRepository.findAll().stream()
                .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        int safeTimelineOffsetHours = normalizeTimelineOffsetHours(timelineOffsetHours);
        LocalDateTime baseTimelineStart = floorToHalfHour(LocalDateTime.now());
        LocalDateTime timelineStart = baseTimelineStart.plusHours(safeTimelineOffsetHours);
        LocalDateTime timelineEnd = timelineStart.plusHours(24);

        List<TimelineSlotView> roomTimelineSlots = roomTimelineService.buildRoomTimelineSlots(timelineStart, 48);
        List<RoomTimelineRowView> roomTimelineRows = roomTimelineService.buildRoomTimelineRows(allRooms, rentals, timelineStart, timelineEnd);

        return ResponseEntity.ok(Map.of(
                "roomTimelineSlots", roomTimelineSlots,
                "roomTimelineRows", roomTimelineRows,
                "timelineStart", timelineStart,
                "timelineEnd", timelineEnd,
                "availableNowRoomCount", roomTimelineRows.stream().filter(RoomTimelineRowView::availableNow).count(),
                "busyNowRoomCount", roomTimelineRows.stream().filter(row -> !row.availableNow() && !row.maintenanceRoom()).count(),
                "maintenanceRoomCount", roomTimelineRows.stream().filter(RoomTimelineRowView::maintenanceRoom).count()
        ));
    }

    @GetMapping("/walk-in/rooms")
    public ResponseEntity<?> getWalkInRooms(@RequestParam(name = "walkInCheckin", required = false) String walkInCheckin,
                                            @RequestParam(name = "walkInLengthOfStay", defaultValue = "1") Integer walkInLengthOfStay) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDateTime baseTimelineStart = floorToHalfHour(LocalDateTime.now());
        int safeWalkInLengthOfStay = walkInLengthOfStay == null || walkInLengthOfStay < MINIMUM_WALK_IN_HOURS ? MINIMUM_WALK_IN_HOURS : walkInLengthOfStay;
        LocalDateTime parsedStart = parseDashboardDateTimeOrDefault(walkInCheckin, baseTimelineStart);
        LocalDateTime walkInSearchStart = parsedStart.isBefore(baseTimelineStart) ? baseTimelineStart : parsedStart;
        LocalDateTime walkInSearchEnd = walkInSearchStart.plusHours(safeWalkInLengthOfStay).plusMinutes(CLEANING_BUFFER_MINUTES);

        List<Room> walkInRooms = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> !rentalOperationService.hasBlockingConflict(room.getId(), walkInSearchStart, walkInSearchEnd, null))
                .toList();

        return ResponseEntity.ok(Map.of("walkInRooms", walkInRooms));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings() {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Rental> rentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Rental> pendingBookings = rentals.stream()
                .filter(rental -> Boolean.TRUE.equals(rental.getIsBooking()) && rental.getStatus() == RentalStatus.PENDING)
                .toList();

        List<Rental> todayArrivals = rentals.stream()
                .filter(rental -> Boolean.TRUE.equals(rental.getIsBooking()) && rental.getStatus() == RentalStatus.CONFIRMED)
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .filter(rental -> !rental.getRentalDetails().isEmpty() && rental.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(rental -> rental.getRentalDetails().get(0).getCheckinDate().toLocalDate().isEqual(LocalDate.now()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "pendingBookings", pendingBookings,
                "todayArrivals", todayArrivals
        ));
    }

    @GetMapping("/rentals/active")
    public ResponseEntity<?> getActiveRentals() {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Rental> rentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Rental> checkedInRentals = rentals.stream()
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN))
                .toList();

        List<Rental> overdueRentals = rentals.stream()
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.OVERDUE))
                .toList();

        List<Service> activeServices = serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE);

        Map<String, BillingSummary> billingSummaries = new HashMap<>();
        for (Rental r : checkedInRentals) billingSummaries.put(r.getId(), billingService.buildBillingSummary(r));
        for (Rental r : overdueRentals) billingSummaries.put(r.getId(), billingService.buildBillingSummary(r));

        return ResponseEntity.ok(Map.of(
                "checkedInRentals", checkedInRentals,
                "overdueRentals", overdueRentals,
                "activeServices", activeServices,
                "billingSummaries", billingSummaries
        ));
    }

    @GetMapping("/rentals/cleaning")
    public ResponseEntity<?> getCleaningRooms() {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Rental> checkedOutRentals = rentalRepository.findAll().stream()
                .filter(rental -> rental.getStatus() == RentalStatus.COMPLETED)
                .filter(rental -> rental.getRentalDetails().stream().anyMatch(d -> d.getRoom() != null && d.getRoom().getStatus() == RoomStatus.CLEANING))
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return ResponseEntity.ok(Map.of("checkedOutRentals", checkedOutRentals));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Map<String, Object>> billList = billRepository.findAllByOrderByCreateDateDesc().stream()
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId());
                    map.put("createDate", b.getCreateDate());
                    map.put("type", b.getType());
                    map.put("totalAmount", b.getTotalAmount());
                    return map;
                }).toList();

        List<Map<String, Object>> paymentList = paymentRepository.findAllByOrderByDateDesc().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("date", p.getDate());
                    map.put("transaction", p.getTransaction());
                    map.put("method", p.getMethod());
                    map.put("bill", p.getBill() != null ? Map.of("id", p.getBill().getId()) : null);
                    return map;
                }).toList();

        List<Map<String, Object>> serviceUsageList = serviceUsageRepository.findAllByOrderByTimeDesc().stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("time", u.getTime());
                    map.put("count", u.getCount());
                    map.put("unitPrice", u.getUnitPrice());
                    map.put("service", u.getService() != null ? Map.of("name", u.getService().getName()) : null);
                    return map;
                }).toList();

        return ResponseEntity.ok(Map.of(
                "bills", billList,
                "payments", paymentList,
                "serviceUsages", serviceUsageList
        ));
    }

    @PostMapping("/walk-in")
    @Transactional
    public ResponseEntity<?> createWalkInRental(@RequestBody com.hotel.system.dto.staff.WalkInRequest request) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            List<String> roomIds = request.getRoomId();
            List<Integer> adultCounts = request.getAdultCount();
            List<Integer> childrenCounts = request.getChildrenCount();
            
            RentalType type = request.getRentalType() != null ? request.getRentalType() : RentalType.HOURLY;
            Integer inputLength = request.getLengthOfStay(); // Nếu HOURLY là số giờ, nếu DAILY là số đêm
            
            if (roomIds == null || roomIds.isEmpty()) throw new RuntimeException("Vui lòng chọn ít nhất 1 phòng.");
            if (inputLength == null || inputLength < 1) throw new RuntimeException("Số lượng giờ/đêm tối thiểu là 1.");

            LocalDateTime checkIn = request.getCheckIn() != null ? request.getCheckIn() : LocalDateTime.now();
            
            if (checkIn.isBefore(LocalDateTime.now().minusMinutes(5))) {
                throw new RuntimeException("Thời gian nhận phòng không được nằm trong quá khứ.");
            }

            LocalDateTime checkOut;
            int lengthOfStay;
            
            if (type == RentalType.DAILY) {
                // Thuê qua đêm: Mặc định checkout 12h trưa hôm sau (cộng thêm số đêm khách chọn)
                checkOut = checkIn.toLocalDate().plusDays(inputLength).atTime(12, 0);
                if (checkOut.isBefore(checkIn)) {
                    checkOut = checkOut.plusDays(1); // Xử lý nếu lố 12h trưa
                }
                lengthOfStay = (int) Math.max(1, ChronoUnit.HOURS.between(checkIn, checkOut));
            } else {
                // Thuê giờ
                lengthOfStay = inputLength;
                checkOut = checkIn.plusHours(lengthOfStay);
            }

            // Kiểm tra xung đột cho tất cả các phòng
            for (String rId : roomIds) {
                if (rentalOperationService.hasBlockingConflict(rId, checkIn, checkOut.plusMinutes(30), null)) {
                    throw new RuntimeException("Phòng đã bị đặt trong khung giờ này. Vui lòng tải lại trang.");
                }
            }
            
            String existingCustomerPid = request.getExistingCustomerPid();
            Customer customer = null;
            if (existingCustomerPid != null && !existingCustomerPid.trim().isEmpty()) {
                Users user = usersRepository.findByPid(existingCustomerPid.trim()).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng."));
                customer = customerRepository.findByUser(user).orElseThrow();
            } else {
                String pid = request.getPid();
                if (pid == null || pid.trim().isEmpty()) throw new RuntimeException("Vui lòng nhập CCCD.");
                if (usersRepository.findByPid(pid.trim()).isPresent()) throw new RuntimeException("CCCD/Hộ chiếu đã có trên hệ thống!");

                String email = request.getEmail();
                if (email == null || email.trim().isEmpty()) {
                    throw new RuntimeException("Vui lòng nhập Email của khách hàng.");
                }
                if (usersRepository.findByEmail(email.trim()).isPresent()) {
                    throw new RuntimeException("Email này đã được sử dụng bởi một tài khoản khác!");
                }

                Users newUser = new Users();
                newUser.setId(AppUtils.generateId("USR", 10));
                newUser.setLastName(request.getLastName());
                newUser.setFirstName(request.getFirstName());
                newUser.setEmail(email.trim());
                newUser.setPid(pid);
                newUser.setPhoneNumber(request.getPhoneNumber());
                newUser.setNationality(request.getNationality());
                if (request.getSex() != null) newUser.setSex(Gender.valueOf(request.getSex().toUpperCase()));
                if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) newUser.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
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
                accStatus.setId(AppUtils.generateId("AST", 10));
                accStatus.setAccount(acc);
                accStatus.setName(AccountState.ACTIVE);
                accStatus.setStartTime(LocalDateTime.now());
                accStatus.setReason("Tạo tài khoản tự động");
                accountStatusRepository.save(accStatus);
            }

            Rental rental = new Rental();
            rental.setId(AppUtils.generateId("REN", 10));
            rental.setRentDate(LocalDateTime.now());
            rental.setIsBooking(true);
            rental.setStatus(RentalStatus.PENDING); 
            rental.setCustomer(customer);
            rental = rentalRepository.save(rental);

            for (int i = 0; i < roomIds.size(); i++) {
                Room room = roomRepository.findById(roomIds.get(i)).orElseThrow();
                RentalDetail detail = new RentalDetail();
                detail.setId(AppUtils.generateId("RDT", 10));
                detail.setRental(rental);
                detail.setRoom(room);
                detail.setCheckinDate(checkIn);
                detail.setLengthOfStay(lengthOfStay);
                detail.setRentalType(type); // Gán hình thức thuê
                
                double basePrice = 0.0;
                if (type == RentalType.DAILY) {
                    basePrice = room.getRoomType().getDailyPrice() != null ? room.getRoomType().getDailyPrice() : 
                                (room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() * 22 : 0.0);
                } else {
                    basePrice = room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() : 0.0;
                }
                
                detail.setAdultCount(adultCounts.get(i));
                detail.setChildrenCount(childrenCounts.get(i));
                detail.setUnitPrice(basePrice);
                detail.setStatus(RentalDetailStatus.PENDING); 
                rentalDetailRepository.save(detail);
            }
            return ResponseEntity.ok(Map.of("message", "Tạo đơn thành công! Đơn đang ở danh sách 'Chờ cọc'."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo đơn: " + e.getMessage()));
        }
    }

    @PostMapping("/bookings/{rentalId}/pay-deposit")
    @Transactional
    public ResponseEntity<?> payDepositCash(@PathVariable String rentalId) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rental rental = rentalRepository.findById(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Đơn không hợp lệ hoặc đã được xử lý."));
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

        return ResponseEntity.ok(Map.of("message", "Đã thu tiền cọc (Tiền mặt) thành công. Khách đã có thể Check-in."));
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
        
        // CHỖ FIX LỖI: Cập nhật 2 trường phụ thu mới thay cho dòng cũ bị gạch đỏ
        finalBill.setEarlyCheckinSurcharge(billingSummary.earlyCheckinSurcharge());
        finalBill.setLateCheckoutSurcharge(billingSummary.lateCheckoutSurcharge());
        
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

    private LocalDateTime floorToHalfHour(LocalDateTime value) {
        if (value == null) return null;
        int flooredMinute = value.getMinute() < 30 ? 0 : 30;
        return value.withMinute(flooredMinute).withSecond(0).withNano(0);
    }

    @GetMapping("/customers/by-pid/{pid}")
    public ResponseEntity<?> getCustomerByPid(@PathVariable String pid) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Users> userOpt = usersRepository.findByPid(pid.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy khách hàng."));
        }

        Users user = userOpt.get();
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Tài khoản không thuộc về khách hàng."));
        }

        return ResponseEntity.ok(customerOpt.get());
    }
}