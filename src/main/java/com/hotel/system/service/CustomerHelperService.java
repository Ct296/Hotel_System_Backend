package com.hotel.system.service;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RentalType;
import com.hotel.system.entity.enums.RoomStatus;
import com.hotel.system.repository.AppliedPeriodRepository;
import com.hotel.system.repository.RentalRepository;
import com.hotel.system.repository.RoomImageRepository;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerHelperService {

    private static final Set<RentalDetailStatus> BLOCKING_BOOKING_STATUSES = EnumSet.of(
            RentalDetailStatus.PENDING, RentalDetailStatus.CHECKED_IN, RentalDetailStatus.OVERDUE
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${hotel.room.cleaning-buffer-minutes:30}")
    private long cleaningBufferMinutes;

    @Value("${hotel.stay.minimum-hours:1}")
    private int minimumStayHours;

    private final RoomImageRepository roomImageRepository;
    private final RentalRepository rentalRepository;
    private final AppliedPeriodRepository appliedPeriodRepository;
    private final RentalOperationService rentalOperationService;

    public record RoomCardView(Room room, double displayPrice, String roomTypeName, double basePrice, String eventName, double surchargeAmount) {}
    public record HomeRoomTypeView(RoomType roomType, double displayPrice, double basePrice, String eventName, double surchargeAmount) {}

    public boolean isRoomAvailable(Room room, LocalDateTime checkIn, LocalDateTime checkOut) {
        return findBlockingRentalConflict(room, checkIn, checkOut) == null;
    }

    public Rental findBlockingRentalConflict(Room room, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (room == null || room.getStatus() != RoomStatus.AVAILABLE || checkIn == null || checkOut == null) return new Rental();

        LocalDateTime requestedStart = checkIn;
        LocalDateTime requestedEnd = checkOut.plusMinutes(cleaningBufferMinutes);

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

    public String buildConflictRange(Rental rental) {
        RentalDetail firstDetail = (rental != null && rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty()) ? rental.getRentalDetails().get(0) : null;
        if (firstDetail == null || firstDetail.getCheckinDate() == null || firstDetail.getLengthOfStay() == null) return "không xác định";
        LocalDateTime end = rentalOperationService.calculateRentalEndWithBuffer(rental);
        return end == null ? "không xác định" : firstDetail.getCheckinDate().format(DATE_TIME_FORMATTER) + " đến " + end.format(DATE_TIME_FORMATTER);
    }

    public int calculateStayHours(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) return minimumStayHours;
        long stayHours = ChronoUnit.HOURS.between(checkIn, checkOut);
        return stayHours < minimumStayHours ? minimumStayHours : (int) stayHours;
    }

    public double calculateRoomSubTotal(double roomUnitPrice, LocalDateTime checkIn, LocalDateTime checkOut, RentalType rentalType) {
        if (checkIn == null || checkOut == null) return 0.0;

        if (rentalType == RentalType.DAILY) {
            long baseNights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
            baseNights = Math.max(0, baseNights);

            double earlyCiSurcharge = 0.0;
            int ciHour = checkIn.getHour();
            if (ciHour < 5) earlyCiSurcharge = 1.0;
            else if (ciHour < 9) earlyCiSurcharge = 0.5;
            else if (ciHour < 14) earlyCiSurcharge = 0.3;

            double lateCoSurcharge = 0.0;
            int coMinutes = checkOut.getHour() * 60 + checkOut.getMinute();
            if (coMinutes > 1080) lateCoSurcharge = 1.0;
            else if (coMinutes > 900) lateCoSurcharge = 0.5;
            else if (coMinutes > 720) lateCoSurcharge = 0.3;

            double totalMultiplier = baseNights + earlyCiSurcharge + lateCoSurcharge;
            if (totalMultiplier <= 0) totalMultiplier = 1.0;

            return Math.max(roomUnitPrice, 0.0) * totalMultiplier;
        }

        long stayMinutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        long fullHours = stayMinutes / 60;
        int finalHours = Math.max(minimumStayHours, (int) fullHours + (stayMinutes % 60 > 30 ? 1 : 0));
        return Math.max(roomUnitPrice, 0.0) * finalHours;
    }

    public double resolveDepositPercent(Room room) {
        if (room == null || room.getRoomType() == null || room.getRoomType().getDepositPercent() == null) return 0.0;
        double dp = room.getRoomType().getDepositPercent();
        return dp < 0 ? 0.0 : (dp > 100 ? 100.0 : dp);
    }

    public double calculateDepositAmount(double totalAmount, double depositPercent) {
        double dp = depositPercent < 0 ? 0.0 : (depositPercent > 100 ? 100.0 : depositPercent);
        return Math.max(totalAmount, 0.0) * dp / 100.0;
    }

    public double resolveRoomUnitPrice(Room room, LocalDateTime checkInDate, RentalType rentalType) {
        if (room == null || room.getRoomType() == null) return 0.0;

        double basePrice = 0.0;
        if (rentalType == RentalType.DAILY) {
            basePrice = room.getRoomType().getDailyPrice() != null ? room.getRoomType().getDailyPrice() :
                        (room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice() * 22 : 0.0);
        } else {
            basePrice = room.getRoomType().getBasePrice() == null ? 0.0 : room.getRoomType().getBasePrice();
        }

        if (room.getRoomType().getId() == null || checkInDate == null) return basePrice;

        AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(room.getRoomType().getId())
                .stream().filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !checkInDate.isBefore(p.getStartDate()) && !checkInDate.isAfter(p.getEndDate()))
                .findFirst().orElse(null);

        if (matchedPeriod != null && matchedPeriod.getPriceRate() != null && matchedPeriod.getPriceRate().getSurchargeAmount() != null) {
            double percent = matchedPeriod.getPriceRate().getSurchargeAmount();
            double surchargeMoney = basePrice * (percent / 100.0);
            return basePrice + surchargeMoney;
        }
        return basePrice;
    }

    public LocalDateTime getMinimumBookingStartTime() {
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        return LocalDateTime.now().isAfter(now) ? now.plusHours(1) : now;
    }

    public double getSafeBasePrice(RoomType roomType) {
        return roomType == null || roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();
    }

    public Comparator<RoomCardView> resolveRoomSort(String sortBy) {
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

    public List<String> getRoomGalleryPaths(Room room) {
        if (room == null) return List.of(getDefaultRoomImagePath());
        List<String> paths = roomImageRepository.findAllByRoomOrderByIsPrimaryDesc(room).stream()
                .map(RoomImage::getImagePath).filter(StringUtils::hasText).distinct().toList();
        return paths.isEmpty() ? List.of(getDefaultRoomImagePath()) : paths;
    }

    private String normalizeRoomSort(String sortBy) {
        String normalized = AppUtils.safeTrim(sortBy).toLowerCase();
        return switch (normalized) {
            case "floor_desc", "floor_asc", "price_desc", "price_asc", "tier_desc", "tier_asc" -> normalized;
            default -> "default";
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

    private String getDefaultRoomImagePath() {
        return "/image/default_room.jpg";
    }
}