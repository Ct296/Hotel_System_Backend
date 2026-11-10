package com.hotel.system.service;

import com.hotel.system.dto.billing.BillingSummary;
import com.hotel.system.entity.AppliedPeriod;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.RoomType;
import com.hotel.system.entity.enums.BillType;
import com.hotel.system.entity.enums.RentalType;
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
            return new BillingSummary(0.0, 0.0, 0.0, 0.0, 0.0, MINIMUM_WALK_IN_HOURS, 0.0, 0.0, 1.0, BillType.FINAL);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestCheckin = rental.getRentalDetails().stream()
                .map(RentalDetail::getCheckinDate).filter(d -> d != null)
                .min(LocalDateTime::compareTo).orElse(now);
                
        LocalDateTime effectiveCheckOut = now.isBefore(earliestCheckin) 
                ? earliestCheckin.plusHours(rental.getRentalDetails().stream().mapToInt(d -> normalizeStayLength(d.getLengthOfStay())).max().orElse(MINIMUM_WALK_IN_HOURS)) 
                : now;

        int actualStayHours = resolveActualStayHours(rental);
        int maxPlannedStayHours = rental.getRentalDetails().stream()
                .mapToInt(d -> normalizeStayLength(d.getLengthOfStay()))
                .max().orElse(MINIMUM_WALK_IN_HOURS);

        double totalRoomAmount = 0.0;
        double totalEarlyCheckinSurcharge = 0.0;
        double totalLateCheckoutSurcharge = 0.0;
        
        for (RentalDetail detail : rental.getRentalDetails()) {
            RentalType type = detail.getRentalType() != null ? detail.getRentalType() : RentalType.HOURLY;
            double unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : resolveRoomUnitPrice(detail.getRoom(), detail.getCheckinDate(), type);
            
            LocalDateTime ci = detail.getCheckinDate() != null ? detail.getCheckinDate() : earliestCheckin;
            LocalDateTime co = now.isBefore(ci) ? ci.plusHours(normalizeStayLength(detail.getLengthOfStay())) : effectiveCheckOut;
            
            // Tính toán chi tiết
            double[] breakdown = calculateRoomSubTotalBreakdown(unitPrice, ci, co, type);
            totalRoomAmount += breakdown[0]; // Tiền phòng + Phụ thu
            totalEarlyCheckinSurcharge += breakdown[1]; // Chỉ lấy tiền phụ thu nhận sớm
            totalLateCheckoutSurcharge += breakdown[2]; // Chỉ lấy tiền phụ thu trả muộn
        }

        double completionRatio = maxPlannedStayHours <= 0 ? 1.0 : (double) actualStayHours / maxPlannedStayHours;
        BillType billType = actualStayHours < maxPlannedStayHours ? BillType.EARLY_CHECKOUT : BillType.FINAL;

        double serviceAmount = calculateServiceAmount(rental);
        double depositPaid = calculateDepositPaid(rental.getId());
        double finalAmount = Math.max(0.0, totalRoomAmount + serviceAmount - depositPaid);

        return new BillingSummary(totalRoomAmount, serviceAmount, depositPaid, calculateDepositAmount(rental), finalAmount,
                actualStayHours, totalEarlyCheckinSurcharge, totalLateCheckoutSurcharge, completionRatio, billType); 
    }

    public double calculateDepositAmount(Rental rental) {
        if (rental == null || rental.getRentalDetails() == null) return 0.0;
        
        double totalDeposit = 0.0;
        for (RentalDetail detail : rental.getRentalDetails()) {
            RentalType type = detail.getRentalType() != null ? detail.getRentalType() : RentalType.HOURLY;
            int stayLength = normalizeStayLength(detail.getLengthOfStay());
            double unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : resolveRoomUnitPrice(detail.getRoom(), detail.getCheckinDate(), type);
            
            LocalDateTime ci = detail.getCheckinDate() != null ? detail.getCheckinDate() : LocalDateTime.now();
            LocalDateTime co = ci.plusHours(stayLength);
            
            double roomAmount = calculateRoomSubTotalBreakdown(unitPrice, ci, co, type)[0];
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

    // Đổi từ private sang public và sửa công thức tính theo %
    public double resolveRoomUnitPrice(Room room, LocalDateTime effectiveMoment, RentalType rentalType) {
        if (room == null || room.getRoomType() == null) return 0.0;
        RoomType roomType = room.getRoomType();
        
        double basePrice;
        if (rentalType == RentalType.DAILY) {
            basePrice = roomType.getDailyPrice() != null ? roomType.getDailyPrice() : 
                        (roomType.getBasePrice() != null ? roomType.getBasePrice() * 22 : 0.0);
        } else {
            basePrice = roomType.getBasePrice() == null ? 0.0 : roomType.getBasePrice();
        }

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

        // SỬA BUG: Tính theo % thay vì cộng tiền mặt trực tiếp
        double surchargePercent = matchedPeriod.getPriceRate().getSurchargeAmount();
        double finalPrice = basePrice + (basePrice * (surchargePercent / 100.0));
        return Math.max(0.0, finalPrice);
    }

    // Gộp luôn 2 hàm resolveDepositPercent & resolveDepositPercentValue bị trùng ở cuối file:
    private double resolveDepositPercent(RoomType roomType) {
        if (roomType == null || roomType.getDepositPercent() == null) return 0.0;
        return Math.max(0.0, Math.min(100.0, roomType.getDepositPercent()));
    }

    private double calculateDepositAmount(double totalRoomAmount, double depositPercent) {
        double validPercent = Math.max(0.0, Math.min(100.0, depositPercent));
        return Math.max(totalRoomAmount, 0.0) * validPercent / 100.0;
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

    private int normalizeStayLength(Integer stayLength) {
        return (stayLength == null || stayLength < MINIMUM_WALK_IN_HOURS) ? MINIMUM_WALK_IN_HOURS : stayLength;
    }

    // LOGIC CHỐT: BÓC TÁCH CHÍNH XÁC TIỀN PHỤ THU
    // Trả về mảng: [0] = Tổng tiền phòng, [1] = Tiền phụ thu vào sớm, [2] = Tiền phụ thu ra muộn
    private double[] calculateRoomSubTotalBreakdown(double roomUnitPrice, LocalDateTime checkIn, LocalDateTime checkOut, RentalType rentalType) {
        if (checkIn == null || checkOut == null) return new double[]{0.0, 0.0, 0.0};
        
        double price = Math.max(roomUnitPrice, 0.0);
        
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
            if (coMinutes > 1080) lateCoSurcharge = 1.0; // Sau 18:00
            else if (coMinutes > 900) lateCoSurcharge = 0.5; // Sau 15:00
            else if (coMinutes > 720) lateCoSurcharge = 0.3; // Sau 12:00

            double totalMultiplier = baseNights + earlyCiSurcharge + lateCoSurcharge;
            if (totalMultiplier <= 0) totalMultiplier = 1.0; 
            
            double totalRoom = price * totalMultiplier;
            double earlySurchargeMoney = price * earlyCiSurcharge;
            double lateSurchargeMoney = price * lateCoSurcharge;
            
            return new double[]{totalRoom, earlySurchargeMoney, lateSurchargeMoney};
        }
        
        // Loại hình HOURLY không có phụ thu
        long stayMinutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        long fullHours = stayMinutes / 60;
        int finalHours = Math.max(MINIMUM_WALK_IN_HOURS, (int) fullHours + (stayMinutes % 60 > 30 ? 1 : 0));
        return new double[]{price * finalHours, 0.0, 0.0};
    }

}