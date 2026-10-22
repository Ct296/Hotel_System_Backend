package com.hotel.system.service;

import com.hotel.system.dto.billing.BillingSummary;
import com.hotel.system.entity.AppliedPeriod;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.enums.BillType;
import com.hotel.system.repository.AppliedPeriodRepository;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.repository.PaymentRepository;
import com.hotel.system.repository.ServiceUsageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BillingService {

    private static final int MINIMUM_WALK_IN_HOURS = 1;
    private static final int SHORT_STAY_SURCHARGE_THRESHOLD_HOURS = 6;
    private static final double SHORT_STAY_SURCHARGE_PERCENT = 10.0;
    private static final double EARLY_CHECKOUT_THRESHOLD_RATIO = 0.8;
    private static final double EARLY_CHECKOUT_PENALTY_PERCENT = 10.0;

    private final AppliedPeriodRepository appliedPeriodRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;

    public BillingService(AppliedPeriodRepository appliedPeriodRepository,
                          ServiceUsageRepository serviceUsageRepository,
                          BillRepository billRepository,
                          PaymentRepository paymentRepository) {
        this.appliedPeriodRepository = appliedPeriodRepository;
        this.serviceUsageRepository = serviceUsageRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
    }

    public BillingSummary buildBillingSummary(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null || rental.getRentalDetails().isEmpty()) {
            return new BillingSummary(0.0, 0.0, 0.0, 0.0, 0.0, MINIMUM_WALK_IN_HOURS, 0.0, 1.0, BillType.FINAL);
        }

        int actualStayHours = resolveActualStayHours(rental);
        int maxPlannedStayHours = rental.getRentalDetails().stream()
                .mapToInt(d -> normalizeStayLength(d.getLengthOfStay()))
                .max().orElse(MINIMUM_WALK_IN_HOURS);
        double totalRoomAmount = 0.0;

        for (RentalDetail detail : rental.getRentalDetails()) {
            double unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : resolveRoomUnitPrice(detail.getRoom(), detail.getCheckinDate());
            double subTotal = calculateRoomSubTotal(unitPrice, actualStayHours);
            totalRoomAmount += (subTotal + calculateShortStaySurcharge(subTotal, actualStayHours));
        }

        double completionRatio = maxPlannedStayHours <= 0 ? 1.0 : (double) actualStayHours / maxPlannedStayHours;
        double earlyCheckoutPenaltyPercent = 0.0;
        BillType billType = BillType.FINAL;

        if (isEarlyCheckout(rental, actualStayHours)) {
            billType = BillType.EARLY_CHECKOUT;
            if (completionRatio < EARLY_CHECKOUT_THRESHOLD_RATIO) {
                earlyCheckoutPenaltyPercent = EARLY_CHECKOUT_PENALTY_PERCENT;
                totalRoomAmount += totalRoomAmount * earlyCheckoutPenaltyPercent / 100.0;
            }
        }

        double serviceAmount = calculateServiceAmount(rental);
        double depositPaid = calculateDepositPaid(rental.getId());
        double finalAmount = Math.max(0.0, totalRoomAmount + serviceAmount - depositPaid);

        return new BillingSummary(totalRoomAmount, serviceAmount, depositPaid, calculateDepositAmount(rental), finalAmount,
                actualStayHours, earlyCheckoutPenaltyPercent, completionRatio, billType);
    }

    public double calculateDepositAmount(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null) return 0.0;
        double totalDeposit = 0.0;
        for (RentalDetail detail : rental.getRentalDetails()) {
            int stayLength = normalizeStayLength(detail.getLengthOfStay());
            double unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : resolveRoomUnitPrice(detail.getRoom(), detail.getCheckinDate());
            double subTotal = calculateRoomSubTotal(unitPrice, stayLength);
            double roomAmount = subTotal + calculateShortStaySurcharge(subTotal, stayLength);
            double depositPct = resolveDepositPercent(detail.getRoom() != null ? detail.getRoom().getRoomType() : null);
            totalDeposit += calculateDepositAmount(roomAmount, depositPct);
        }
        return totalDeposit;
    }

    public double calculateDepositPaid(String rentalId) {
        return billRepository.findByRentalIdOrderByCreateDateDesc(rentalId).stream()
                .filter(item -> item.getType() == BillType.DEPOSIT)
                .mapToDouble(item -> item.getTotalAmount() == null ? 0.0 : item.getTotalAmount())
                .sum();
    }

    public boolean hasPaymentForBillType(String rentalId, BillType billType) {
        return paymentRepository.findByBillRentalIdOrderByDateDesc(rentalId).stream()
                .anyMatch(payment -> payment.getBill() != null && payment.getBill().getType() == billType);
    }

    public boolean hasSettlementBill(String rentalId) {
        return billRepository.existsByRentalIdAndType(rentalId, BillType.FINAL)
                || billRepository.existsByRentalIdAndType(rentalId, BillType.EARLY_CHECKOUT);
    }

    public boolean hasSettlementPayment(String rentalId) {
        return hasPaymentForBillType(rentalId, BillType.FINAL)
                || hasPaymentForBillType(rentalId, BillType.EARLY_CHECKOUT);
    }

    private double resolveRoomUnitPrice(Room room, LocalDateTime effectiveMoment) {
        if (room == null || room.getRoomType() == null) return 0.0;
        RoomType roomType = room.getRoomType();
        double basePrice = roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();
        if (effectiveMoment == null || roomType.getId() == null) return basePrice;

        AppliedPeriod matchedPeriod = appliedPeriodRepository.findByRoomTypeIdOrderByStartDateDesc(roomType.getId())
                .stream()
                .filter(period -> period.getStartDate() != null && period.getEndDate() != null)
                .filter(period -> !effectiveMoment.isBefore(period.getStartDate()) && !effectiveMoment.isAfter(period.getEndDate()))
                .findFirst()
                .orElse(null);

        if (matchedPeriod == null || matchedPeriod.getPriceRate() == null || matchedPeriod.getPriceRate().getSurchargeAmount() == null) {
            return basePrice;
        }
        return basePrice + matchedPeriod.getPriceRate().getSurchargeAmount();
    }

    private double calculateServiceAmount(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null) return 0.0;
        List<String> detailIds = rental.getRentalDetails().stream().map(RentalDetail::getId).toList();
        
        return serviceUsageRepository.findAllByOrderByTimeDesc().stream()
                .filter(item -> item.getRentalDetail() != null && detailIds.contains(item.getRentalDetail().getId()))
                .mapToDouble(item -> {
                    int count = item.getCount() == null ? 0 : item.getCount();
                    double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : (item.getService() == null || item.getService().getBasePrice() == null ? 0.0 : item.getService().getBasePrice());
                    return unitPrice * count;
                }).sum();
    }

    public int resolveActualStayHours(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null) return MINIMUM_WALK_IN_HOURS;
        
        LocalDateTime earliestCheckin = rental.getRentalDetails().stream()
                .map(RentalDetail::getCheckinDate).filter(d -> d != null)
                .min(LocalDateTime::compareTo).orElse(null);
                
        if (earliestCheckin == null) return MINIMUM_WALK_IN_HOURS;
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(earliestCheckin)) {
            return rental.getRentalDetails().stream().mapToInt(d -> normalizeStayLength(d.getLengthOfStay())).max().orElse(MINIMUM_WALK_IN_HOURS);
        }
        
        long totalMinutes = ChronoUnit.MINUTES.between(earliestCheckin, now);
        if (totalMinutes <= 0) return MINIMUM_WALK_IN_HOURS;
        
        long fullHours = totalMinutes / 60;
        return Math.max(MINIMUM_WALK_IN_HOURS, (int) fullHours + (totalMinutes % 60 > 30 ? 1 : 0));
    }

    private boolean isEarlyCheckout(Rental rental, int actualStayHours) {
        RentalDetail firstDetail = (rental != null && rental.getRentalDetails() != null && !rental.getRentalDetails().isEmpty()) 
                                    ? rental.getRentalDetails().get(0) : null;
        return firstDetail != null && actualStayHours < normalizeStayLength(firstDetail.getLengthOfStay());
    }

    private int normalizeStayLength(Integer stayLength) {
        return (stayLength == null || stayLength < MINIMUM_WALK_IN_HOURS) ? MINIMUM_WALK_IN_HOURS : stayLength;
    }

    private double calculateRoomSubTotal(double roomUnitPrice, int stayLength) {
        return Math.max(roomUnitPrice, 0.0) * normalizeStayLength(stayLength);
    }

    private double calculateShortStaySurcharge(double roomSubTotal, int stayLength) {
        if (normalizeStayLength(stayLength) < SHORT_STAY_SURCHARGE_THRESHOLD_HOURS) {
            return roomSubTotal * SHORT_STAY_SURCHARGE_PERCENT / 100.0;
        }
        return 0.0;
    }

    private double resolveDepositPercent(RoomType roomType) {
        if (roomType == null || roomType.getDepositPercent() == null) return 0.0;
        double depositPercent = roomType.getDepositPercent();
        if (depositPercent < 0) return 0.0;
        if (depositPercent > 100) return 100.0;
        return depositPercent;
    }

    private double calculateDepositAmount(double totalRoomAmount, double depositPercent) {
        return Math.max(totalRoomAmount, 0.0) * resolveDepositPercentValue(depositPercent) / 100.0;
    }

    private double resolveDepositPercentValue(double depositPercent) {
        if (depositPercent < 0) return 0.0;
        if (depositPercent > 100) return 100.0;
        return depositPercent;
    }
}