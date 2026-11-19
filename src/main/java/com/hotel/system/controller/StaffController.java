package com.hotel.system.controller;

import com.hotel.system.dto.staff.CheckInRoomGuestsRequest;
import com.hotel.system.dto.staff.WalkInRequest;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.StaffBookingService;
import com.hotel.system.service.StaffQueryService;
import com.hotel.system.service.StaffStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffQueryService staffQueryService;
    private final StaffBookingService staffBookingService;
    private final StaffStayService staffStayService;
    private final UsersRepository usersRepository;

    private Users getLoggedInStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent() && (userOpt.get().getRole() == Role.STAFF
                    || userOpt.get().getRole() == Role.MANAGER
                    || userOpt.get().getRole() == Role.ADMIN)) {
                return userOpt.get();
            }
        }
        return null;
    }

    private ResponseEntity<?> executeAuthorized(Supplier<Object> action) {
        if (getLoggedInStaff() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(action.get());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        return executeAuthorized(staffQueryService::getOverviewData);
    }

    @GetMapping("/timeline")
    public ResponseEntity<?> getTimeline(@RequestParam(name = "timelineOffsetHours", defaultValue = "0") Integer timelineOffsetHours) {
        return executeAuthorized(() -> staffQueryService.getTimelineData(timelineOffsetHours));
    }

    @GetMapping("/walk-in/rooms")
    public ResponseEntity<?> getWalkInRooms(@RequestParam(name = "walkInCheckin", required = false) String walkInCheckin,
                                            @RequestParam(name = "walkInLengthOfStay", defaultValue = "1") Integer walkInLengthOfStay) {
        return executeAuthorized(() -> staffQueryService.getWalkInRooms(walkInCheckin, walkInLengthOfStay));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings() {
        return executeAuthorized(staffQueryService::getBookingsData);
    }

    @GetMapping("/rentals/active")
    public ResponseEntity<?> getActiveRentals() {
        return executeAuthorized(staffQueryService::getActiveRentalsData);
    }

    @GetMapping("/rentals/cleaning")
    public ResponseEntity<?> getCleaningRooms() {
        return executeAuthorized(staffQueryService::getCleaningRoomsData);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return executeAuthorized(staffQueryService::getHistoryData);
    }

    @GetMapping("/customers/by-pid/{pid}")
    public ResponseEntity<?> getCustomerByPid(@PathVariable String pid) {
        return executeAuthorized(() -> staffQueryService.findCustomerByPid(pid));
    }

    @PostMapping("/walk-in")
    public ResponseEntity<?> createWalkInRental(@RequestBody WalkInRequest request) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.createWalkInRental(request)));
    }

    @PostMapping("/bookings/{rentalId}/pay-deposit")
    public ResponseEntity<?> payDepositCash(@PathVariable String rentalId) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.payDepositCash(rentalId)));
    }

    @PostMapping("/bookings/{rentalId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String rentalId) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.cancelBooking(rentalId)));
    }

    @PostMapping("/bookings/{rentalId}/cancel-refund")
    public ResponseEntity<?> cancelBookingWithRefund(@PathVariable String rentalId,
                                                     @RequestBody(required = false) Map<String, String> payload) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.cancelBookingWithRefund(rentalId, payload)));
    }

    @PostMapping("/rentals/{rentalId}/check-in")
    public ResponseEntity<?> checkIn(@PathVariable String rentalId,
                                     @RequestBody(required = false) CheckInRoomGuestsRequest guestRequest) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.checkIn(rentalId, guestRequest)));
    }

    @PostMapping("/rentals/{rentalId}/services")
    public ResponseEntity<?> addServiceToRental(@PathVariable String rentalId, @RequestBody Map<String, Object> payload) {
        return executeAuthorized(() -> Map.of("message", staffStayService.addServiceToRental(rentalId, payload)));
    }

    @DeleteMapping("/service-usages/{usageId}")
    public ResponseEntity<?> removeServiceFromRental(@PathVariable String usageId) {
        return executeAuthorized(() -> Map.of("message", staffStayService.removeServiceFromRental(usageId)));
    }

    @PostMapping("/rentals/{rentalId}/final-payment")
    public ResponseEntity<?> collectFinalPayment(@PathVariable String rentalId,
                                                 @RequestBody(required = false) Map<String, String> payload) {
        return executeAuthorized(() -> staffStayService.collectFinalPayment(rentalId, payload));
    }

    @PostMapping("/rentals/{rentalId}/check-out")
    public ResponseEntity<?> checkOut(@PathVariable String rentalId) {
        return executeAuthorized(() -> Map.of("message", staffStayService.checkOut(rentalId)));
    }

    @PostMapping("/rental-details/{detailId}/change-room")
    public ResponseEntity<?> changeRoom(@PathVariable String detailId, @RequestBody Map<String, Object> payload) {
        return executeAuthorized(() -> Map.of("message", staffStayService.changeRoom(detailId, payload)));
    }

    @PutMapping("/rooms/{roomId}/status/available")
    public ResponseEntity<?> markRoomAvailable(@PathVariable String roomId) {
        return executeAuthorized(() -> Map.of("message", staffStayService.markRoomAvailable(roomId)));
    }

    @PutMapping("/rental-details/{detailId}/guests")
    public ResponseEntity<?> updateRoomGuests(@PathVariable String detailId,
                                              @RequestBody List<CheckInRoomGuestsRequest.GuestItem> guests) {
        return executeAuthorized(() -> Map.of("message", staffBookingService.updateRoomGuests(detailId, guests)));
    }
}