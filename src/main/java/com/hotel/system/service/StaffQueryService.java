package com.hotel.system.service;

import com.hotel.system.dto.billing.BillingSummary;
import com.hotel.system.dto.timeline.RoomTimelineRowView;
import com.hotel.system.dto.timeline.TimelineSlotView;
import com.hotel.system.entity.Customer;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RentalStatus;
import com.hotel.system.entity.enums.RoomStatus;
import com.hotel.system.entity.enums.ServiceStatus;
import com.hotel.system.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StaffQueryService {

    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final int MINIMUM_WALK_IN_HOURS = 1;

    private final RoomTimelineService roomTimelineService;
    private final BillingService billingService;
    private final RentalOperationService rentalOperationService;
    private final RentalRepository rentalRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final UsersRepository usersRepository;
    private final CustomerRepository customerRepository;

    public Map<String, Object> getOverviewData() {
        rentalOperationService.expirePendingBookingsWithoutDeposit();
        rentalOperationService.markCheckedInRentalsAsOverdue();
        rentalOperationService.autoReleaseCleaningRooms();

        List<Rental> rentals = rentalRepository.findAll();

        long pendingCount = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING).count();

        List<Rental> todayArrivalsList = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.CONFIRMED)
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .filter(r -> !r.getRentalDetails().isEmpty() && r.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(r -> r.getRentalDetails().get(0).getCheckinDate().toLocalDate().isEqual(LocalDate.now()))
                .sorted(Comparator.comparing(r -> r.getRentalDetails().get(0).getCheckinDate()))
                .toList();

        long checkedInCount = rentals.stream()
                .filter(r -> r.getRentalDetails().stream()
                        .anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE))
                .count();

        long cleaningCount = roomRepository.findByStatus(RoomStatus.CLEANING).size();
        long availableCount = roomRepository.findByStatus(RoomStatus.AVAILABLE).size();
        long maintenanceCount = roomRepository.findByStatus(RoomStatus.MAINTENANCE).size();
        long totalRooms = roomRepository.count();
        long busyCount = Math.max(0, totalRooms - availableCount - cleaningCount - maintenanceCount);

        return Map.of(
                "pendingBookingCount", pendingCount,
                "todayArrivalsCount", todayArrivalsList.size(),
                "checkedInCount", checkedInCount,
                "checkedOutCount", cleaningCount,
                "todayArrivalsList", todayArrivalsList,
                "roomStatusCounts", List.of(availableCount, busyCount, cleaningCount, maintenanceCount),
                "rooms", roomRepository.findAllByOrderByNameAsc()
        );
    }

    public Map<String, Object> getTimelineData(Integer timelineOffsetHours) {
        List<Rental> rentals = rentalRepository.findAll();
        List<Room> allRooms = roomRepository.findAll().stream()
                .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        int safeOffset = normalizeTimelineOffsetHours(timelineOffsetHours);
        LocalDateTime baseStart = floorToHalfHour(LocalDateTime.now());
        LocalDateTime timelineStart = baseStart.plusHours(safeOffset);
        LocalDateTime timelineEnd = timelineStart.plusHours(24);

        List<TimelineSlotView> slots = roomTimelineService.buildRoomTimelineSlots(timelineStart, 48);
        List<RoomTimelineRowView> rows = roomTimelineService.buildRoomTimelineRows(allRooms, rentals, timelineStart, timelineEnd);

        return Map.of(
                "roomTimelineSlots", slots,
                "roomTimelineRows", rows,
                "timelineStart", timelineStart,
                "timelineEnd", timelineEnd,
                "availableNowRoomCount", rows.stream().filter(RoomTimelineRowView::availableNow).count(),
                "busyNowRoomCount", rows.stream().filter(row -> !row.availableNow() && !row.maintenanceRoom()).count(),
                "maintenanceRoomCount", rows.stream().filter(RoomTimelineRowView::maintenanceRoom).count()
        );
    }

    public Map<String, Object> getWalkInRooms(String walkInCheckin, Integer walkInLengthOfStay) {
        LocalDateTime baseStart = floorToHalfHour(LocalDateTime.now());
        int safeHours = (walkInLengthOfStay == null || walkInLengthOfStay < MINIMUM_WALK_IN_HOURS) ? MINIMUM_WALK_IN_HOURS : walkInLengthOfStay;
        LocalDateTime parsedStart = parseDashboardDateTimeOrDefault(walkInCheckin, baseStart);
        LocalDateTime searchStart = parsedStart.isBefore(baseStart) ? baseStart : parsedStart;
        LocalDateTime searchEnd = searchStart.plusHours(safeHours).plusMinutes(CLEANING_BUFFER_MINUTES);

        List<Room> walkInRooms = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> !rentalOperationService.hasBlockingConflict(room.getId(), searchStart, searchEnd, null))
                .toList();

        return Map.of("walkInRooms", walkInRooms);
    }

    public Map<String, Object> getBookingsData() {
        List<Rental> rentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Map<String, Object>> pendingBookings = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING)
                .map(this::buildBookingMap)
                .toList();

        List<Map<String, Object>> todayArrivals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.CONFIRMED)
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .filter(r -> !r.getRentalDetails().isEmpty() && r.getRentalDetails().get(0).getCheckinDate() != null)
                .filter(r -> r.getRentalDetails().get(0).getCheckinDate().toLocalDate().isEqual(LocalDate.now()))
                .map(this::buildBookingMap)
                .toList();

        List<Map<String, Object>> allConfirmedBookings = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.CONFIRMED)
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.PENDING))
                .map(this::buildBookingMap)
                .toList();

        return Map.of(
                "pendingBookings", pendingBookings,
                "todayArrivals", todayArrivals,
                "allConfirmedBookings", allConfirmedBookings
        );
    }

    public Map<String, Object> getActiveRentalsData() {
        List<Rental> rentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Rental> checkedInRentals = rentals.stream()
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN))
                .toList();

        List<Rental> overdueRentals = rentals.stream()
                .filter(r -> r.getRentalDetails().stream().anyMatch(d -> d.getStatus() == RentalDetailStatus.OVERDUE))
                .toList();

        List<com.hotel.system.entity.Service> activeServices = serviceRepository.findByStatusOrderByCreateDateDesc(ServiceStatus.ACTIVE);

        Map<String, BillingSummary> billingSummaries = new HashMap<>();
        Map<String, Map<String, Object>> settlementDetails = new HashMap<>();

        List<Rental> allActive = new ArrayList<>();
        allActive.addAll(checkedInRentals);
        allActive.addAll(overdueRentals);

        for (Rental r : allActive) {
            billingSummaries.put(r.getId(), billingService.buildBillingSummary(r));
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("incidentPenalty", billingService.calculateIncidentPenalty(r));
            detailMap.put("incidentCompensation", billingService.calculateIncidentCompensation(r));
            detailMap.put("refundPaid", billingService.calculateRefundPaid(r.getId()));
            detailMap.put("netPayable", billingService.calculateNetPayable(r));
            detailMap.put("settled", billingService.hasSettlementBill(r.getId()));
            settlementDetails.put(r.getId(), detailMap);
        }

        return Map.of(
                "checkedInRentals", checkedInRentals,
                "overdueRentals", overdueRentals,
                "activeServices", activeServices,
                "billingSummaries", billingSummaries,
                "settlementDetails", settlementDetails
        );
    }

    public Map<String, Object> getCleaningRoomsData() {
        List<Rental> completedRentals = rentalRepository.findAll().stream()
                .filter(r -> r.getStatus() == RentalStatus.COMPLETED)
                .sorted(Comparator.comparing(Rental::getRentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Rental> checkedOutRentals = new ArrayList<>();
        Set<String> processedRoomIds = new HashSet<>();

        for (Rental rental : completedRentals) {
            for (RentalDetail detail : rental.getRentalDetails()) {
                Room room = detail.getRoom();
                if (room != null && room.getStatus() == RoomStatus.CLEANING && processedRoomIds.add(room.getId())) {
                    Rental clone = new Rental();
                    clone.setId(rental.getId());
                    clone.setCustomer(rental.getCustomer());
                    clone.setRentalDetails(List.of(detail));
                    checkedOutRentals.add(clone);
                }
            }
        }
        return Map.of("checkedOutRentals", checkedOutRentals);
    }

    public Map<String, Object> getHistoryData() {
        List<Map<String, Object>> billList = billRepository.findAllByOrderByCreateDateDesc().stream()
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId());
                    map.put("createDate", b.getCreateDate());
                    map.put("type", b.getType());
                    map.put("totalAmount", b.getTotalAmount());
                    map.put("note", b.getNote());
                    return map;
                }).toList();

        List<Map<String, Object>> paymentList = paymentRepository.findAllByOrderByDateDesc().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("date", p.getDate());
                    map.put("transaction", p.getTransaction());
                    map.put("method", p.getMethod());
                    map.put("note", p.getNote());
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
                    map.put("rentalDetailId", u.getRentalDetail() != null ? u.getRentalDetail().getId() : null);
                    return map;
                }).toList();

        return Map.of("bills", billList, "payments", paymentList, "serviceUsages", serviceUsageList);
    }

    public Customer findCustomerByPid(String pid) {
        Users user = usersRepository.findByPid(pid.trim())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng."));
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new NoSuchElementException("Tài khoản không thuộc về khách hàng."));
    }

        private Map<String, Object> buildBookingMap(Rental rental) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rental.getId());
        map.put("isBooking", rental.getIsBooking());
        map.put("rentDate", rental.getRentDate());
        map.put("note", rental.getNote());
        map.put("customer", rental.getCustomer());
        map.put("rentalDetails", rental.getRentalDetails());
        map.put("status", rental.getStatus());
        map.put("depositAmount", billingService.calculateDepositAmount(rental));
        return map;
        }

    private int normalizeTimelineOffsetHours(Integer offset) {
        if (offset == null) return 0;
        int remainder = offset % 24;
        return remainder == 0 ? offset : offset - remainder;
    }

    private LocalDateTime parseDashboardDateTimeOrDefault(String value, LocalDateTime defaultValue) {
        try {
            return (value == null || value.isBlank()) ? defaultValue : LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private LocalDateTime floorToHalfHour(LocalDateTime value) {
        if (value == null) return null;
        return value.withMinute(value.getMinute() < 30 ? 0 : 30).withSecond(0).withNano(0);
    }
}