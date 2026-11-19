package com.hotel.system.service;

import com.hotel.system.dto.billing.BillingSummary;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffStayService {

    private static final long CLEANING_BUFFER_MINUTES = 30;
    private static final int MINIMUM_WALK_IN_HOURS = 1;

    private final BillingService billingService;
    private final CustomerTierService customerTierService;
    private final RentalOperationService rentalOperationService;
    private final EmailService emailService;
    private final RentalRepository rentalRepository;
    private final RentalDetailRepository rentalDetailRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public String addServiceToRental(String rentalId, Map<String, Object> payload) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Rental."));

        String targetDetailId = payload.get("rentalDetailId") != null ? payload.get("rentalDetailId").toString() : null;
        RentalDetail targetDetail = rental.getRentalDetails().stream()
                .filter(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE)
                .filter(d -> targetDetailId == null || targetDetailId.isBlank() || targetDetailId.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng đang lưu trú hợp lệ."));

        com.hotel.system.entity.Service service = serviceRepository.findById(payload.get("serviceId").toString())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ."));

        ServiceUsage usage = new ServiceUsage();
        usage.setId(AppUtils.generateId("SVG", 10));
        usage.setCount(Integer.valueOf(payload.get("quantity").toString()));
        usage.setTime(LocalDateTime.now());
        usage.setUnitPrice(service.getBasePrice());
        usage.setRentalDetail(targetDetail);
        usage.setService(service);
        serviceUsageRepository.save(usage);

        return "Thêm dịch vụ thành công.";
    }

    @Transactional
    public String removeServiceFromRental(String usageId) {
        ServiceUsage usage = serviceUsageRepository.findById(usageId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bản ghi dịch vụ cần hủy."));

        RentalDetail detail = usage.getRentalDetail();
        Rental rental = (detail != null) ? detail.getRental() : null;
        if (rental == null) throw new IllegalArgumentException("Dữ liệu đơn thuê của dịch vụ này không hợp lệ.");

        if (rental.getStatus() == RentalStatus.COMPLETED || rental.getStatus() == RentalStatus.CANCELLED) {
            throw new IllegalArgumentException("Đơn thuê đã kết thúc hoặc đã hủy, không thể xóa dịch vụ.");
        }

        boolean isActive = rental.getRentalDetails().stream()
                .anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE);
        if (!isActive) throw new IllegalArgumentException("Chỉ có thể hủy dịch vụ khi khách đang trong thời gian lưu trú.");

        if (billingService.hasSettlementBill(rental.getId())) {
            throw new IllegalArgumentException("Đơn thuê đã hoàn tất tất toán, không thể hủy dịch vụ đã chốt trên hóa đơn!");
        }

        String serviceName = usage.getService() != null ? usage.getService().getName() : "dịch vụ";
        serviceUsageRepository.delete(usage);
        return "Đã hủy " + serviceName + " khỏi đơn thuê thành công.";
    }

    @Transactional
    public Map<String, Object> collectFinalPayment(String rentalId, Map<String, String> payload) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Rental."));

        boolean isActive = rental.getRentalDetails().stream()
                .anyMatch(d -> d.getStatus() == RentalDetailStatus.CHECKED_IN || d.getStatus() == RentalDetailStatus.OVERDUE);
        if (!isActive) throw new IllegalArgumentException("Chỉ ghi nhận tất toán cho khách đang ở.");

        if (billingService.hasSettlementBill(rentalId)) {
            throw new IllegalArgumentException("Đơn thuê này đã được tất toán trước đó.");
        }

        Map<String, String> safePayload = payload != null ? payload : Map.of();
        String note = safePayload.get("note");
        BillingSummary billingSummary = billingService.buildBillingSummary(rental);
        double netPayable = billingService.calculateNetPayable(rental);

        Bill bill = new Bill();
        bill.setId(AppUtils.generateId("BIL", 10));
        bill.setCreateDate(LocalDateTime.now());
        bill.setActualStayHours(billingSummary.actualStayHours());
        bill.setActualRoomAmount(billingSummary.roomAmount());
        bill.setEarlyCheckinSurcharge(billingSummary.earlyCheckinSurcharge());
        bill.setLateCheckoutSurcharge(billingSummary.lateCheckoutSurcharge());
        bill.setRental(rental);

        String successMessage;
        if (netPayable > 0) {
            PaymentMethod paymentMethod = PaymentMethod.valueOf(safePayload.getOrDefault("paymentMethod", "CASH"));
            bill.setType(BillType.FINAL);
            bill.setTotalAmount(netPayable);
            bill.setNote(note);
            billRepository.save(bill);

            Payment payment = new Payment();
            payment.setId(AppUtils.generateId("PAY", 10));
            payment.setMethod(paymentMethod);
            payment.setDate(LocalDateTime.now());
            payment.setTransaction(AppUtils.buildTransactionCode(paymentMethod, safePayload.get("transactionCode")));
            payment.setNote(note);
            payment.setBill(bill);
            paymentRepository.save(payment);
            successMessage = "Đã ghi nhận thanh toán cuối thành công.";
        } else if (netPayable == 0) {
            bill.setType(BillType.FINAL);
            bill.setTotalAmount(0.0);
            bill.setNote((note != null && !note.isBlank()) ? note : "Tất toán 0đ (Hòa tiền)");
            billRepository.save(bill);
            successMessage = "Đã xác nhận tất toán 0đ (Hòa tiền). Có thể tiến hành Check-out.";
        } else {
            double refundAmount = Math.abs(netPayable);
            PaymentMethod refundMethod = PaymentMethod.valueOf(safePayload.getOrDefault("paymentMethod", "CASH"));
            bill.setType(BillType.REFUND);
            bill.setTotalAmount(refundAmount);
            bill.setNote((note != null && !note.isBlank()) ? note : "Hoàn tiền chênh lệch/bồi thường khi trả phòng");
            billRepository.save(bill);

            Payment refundPayment = new Payment();
            refundPayment.setId(AppUtils.generateId("PAY", 10));
            refundPayment.setMethod(refundMethod);
            refundPayment.setDate(LocalDateTime.now());
            String tx = safePayload.get("transactionCode");
            refundPayment.setTransaction((tx != null && !tx.isBlank()) ? tx.trim() : "REFUND-" + rental.getId());
            refundPayment.setNote(bill.getNote());
            refundPayment.setBill(bill);
            paymentRepository.save(refundPayment);
            successMessage = "Đã lập phiếu hoàn tiền " + AppUtils.formatMoney(refundAmount) + " cho khách. Có thể tiến hành Check-out.";
        }

        customerTierService.syncCustomerTierByBills(rental.getCustomer());
        return Map.of("message", successMessage, "billType", bill.getType(), "amount", bill.getTotalAmount());
    }

    @Transactional
    public String checkOut(String rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Rental."));

        if (!billingService.hasSettlementBill(rentalId)) {
            throw new IllegalArgumentException("Chưa thể check-out. Cần tất toán hoặc xác nhận hoàn tiền trước.");
        }

        rental.setStatus(RentalStatus.COMPLETED);
        rentalRepository.save(rental);

        for (RentalDetail detail : rental.getRentalDetails()) {
            if (detail.getStatus() == RentalDetailStatus.CHECKED_IN || detail.getStatus() == RentalDetailStatus.OVERDUE) {
                if (detail.getRoom() != null) {
                    Room room = detail.getRoom();
                    room.setStatus(RoomStatus.CLEANING);
                    roomRepository.save(room);
                }
                detail.setStatus(RentalDetailStatus.CHECKED_OUT);
                rentalDetailRepository.save(detail);
            }
        }

        sendCheckoutEmail(rental);
        return "Hoàn tất check-out. Các phòng chuyển sang trạng thái đang dọn dẹp.";
    }

    @Transactional
    public String changeRoom(String detailId, Map<String, Object> payload) {
        RentalDetail oldDetail = rentalDetailRepository.findById(detailId).orElse(null);
        if (oldDetail == null || oldDetail.getRental() == null) {
            throw new IllegalArgumentException("Không tìm thấy chi tiết phòng thuê.");
        }

        String newRoomId = payload.get("newRoomId") != null ? payload.get("newRoomId").toString().trim() : "";
        String reasonType = payload.get("reasonType") != null ? payload.get("reasonType").toString().trim().toUpperCase() : "CUSTOMER_REQUEST";
        String note = payload.get("note") != null ? payload.get("note").toString().trim() : "";

        if (newRoomId.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn phòng mới.");
        if (oldDetail.getRoom() != null && newRoomId.equals(oldDetail.getRoom().getId())) {
            throw new IllegalArgumentException("Phòng mới không được trùng với phòng hiện tại.");
        }

        Room newRoom = roomRepository.findById(newRoomId).orElse(null);
        if (newRoom == null || newRoom.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalArgumentException("Phòng mới không tồn tại hoặc hiện không sẵn sàng.");
        }

        RoomType newType = newRoom.getRoomType();
        if (newType != null) {
            if (newType.getAdultMax() != null && oldDetail.getAdultCount() > newType.getAdultMax()) {
                throw new IllegalArgumentException("Phòng mới không đủ sức chứa người lớn (" + newType.getAdultMax() + ").");
            }
            if (newType.getChildrenMax() != null && oldDetail.getChildrenCount() > newType.getChildrenMax()) {
                throw new IllegalArgumentException("Phòng mới không đủ sức chứa trẻ em (" + newType.getChildrenMax() + ").");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        RentalType rentalType = oldDetail.getRentalType() != null ? oldDetail.getRentalType() : RentalType.HOURLY;
        double oldPrice = oldDetail.getUnitPrice() != null ? oldDetail.getUnitPrice() : 0.0;
        double newRoomMarketPrice = billingService.resolveRoomUnitPrice(newRoom, now, rentalType);

        boolean isHotelFault = "HOTEL_FAULT".equals(reasonType);
        double resolvedNewPrice = isHotelFault ? Math.min(oldPrice, newRoomMarketPrice) : newRoomMarketPrice;

        // LUỒNG 1: Khách chưa vào ở (PENDING) -> Ghi đè trực tiếp
        if (oldDetail.getStatus() == RentalDetailStatus.PENDING) {
            LocalDateTime start = oldDetail.getCheckinDate() != null ? oldDetail.getCheckinDate() : now;
            int hours = (oldDetail.getLengthOfStay() != null && oldDetail.getLengthOfStay() > 0) ? oldDetail.getLengthOfStay() : MINIMUM_WALK_IN_HOURS;
            LocalDateTime endWithBuffer = start.plusHours(hours).plusMinutes(CLEANING_BUFFER_MINUTES);

            if (rentalOperationService.hasBlockingConflict(newRoom.getId(), start, endWithBuffer, oldDetail.getRental().getId())) {
                throw new IllegalArgumentException("Phòng mới đã bị trùng lịch trong khung giờ lưu trú.");
            }

            String oldRoomName = oldDetail.getRoom() != null ? oldDetail.getRoom().getName() : "N/A";
            oldDetail.setRoom(newRoom);
            oldDetail.setUnitPrice(resolvedNewPrice);
            oldDetail.setNote("Đổi từ phòng " + oldRoomName + (isHotelFault ? " (Lỗi KS)" : " (Khách yêu cầu)") + (note.isEmpty() ? "" : ": " + note));
            rentalDetailRepository.save(oldDetail);
            return "Đã đổi phòng trước khi nhận từ " + oldRoomName + " sang " + newRoom.getName() + ".";
        }

        // LUỒNG 2: Khách đang ở (CHECKED_IN / OVERDUE) -> Tách chặng (MOVED)
        if (oldDetail.getStatus() == RentalDetailStatus.CHECKED_IN || oldDetail.getStatus() == RentalDetailStatus.OVERDUE) {
            LocalDateTime oldCheckin = oldDetail.getCheckinDate() != null ? oldDetail.getCheckinDate() : now;
            int originalLength = (oldDetail.getLengthOfStay() != null && oldDetail.getLengthOfStay() > 0) ? oldDetail.getLengthOfStay() : MINIMUM_WALK_IN_HOURS;
            LocalDateTime plannedEnd = oldCheckin.plusHours(originalLength);

            int remainingHours = (int) Math.max(MINIMUM_WALK_IN_HOURS, ChronoUnit.HOURS.between(now, plannedEnd));
            LocalDateTime newEndWithBuffer = now.plusHours(remainingHours).plusMinutes(CLEANING_BUFFER_MINUTES);

            if (rentalOperationService.hasBlockingConflict(newRoom.getId(), now, newEndWithBuffer, oldDetail.getRental().getId())) {
                throw new IllegalArgumentException("Phòng mới bị trùng lịch trong khoảng thời gian còn lại.");
            }

            int elapsedBillableHours;
            if (rentalType == RentalType.DAILY) {
                long daysDiff = ChronoUnit.DAYS.between(oldCheckin.toLocalDate(), now.toLocalDate());
                elapsedBillableHours = daysDiff <= 0 ? 0 : (int) Math.max(1, ChronoUnit.HOURS.between(oldCheckin, now));
            } else {
                long mins = ChronoUnit.MINUTES.between(oldCheckin, now);
                elapsedBillableHours = mins <= 15 ? 0 : (int) Math.max(1, (mins + 44) / 60);
            }

            Room oldRoom = oldDetail.getRoom();
            String oldRoomName = oldRoom != null ? oldRoom.getName() : "N/A";

            oldDetail.setLengthOfStay(elapsedBillableHours);
            oldDetail.setStatus(RentalDetailStatus.MOVED);
            oldDetail.setNote("Chuyển sang phòng " + newRoom.getName() + (isHotelFault ? " (Lỗi KS)" : " (Khách yêu cầu)") + (note.isEmpty() ? "" : ": " + note));
            rentalDetailRepository.save(oldDetail);

            if (oldRoom != null) {
                oldRoom.setStatus(isHotelFault ? RoomStatus.MAINTENANCE : RoomStatus.CLEANING);
                roomRepository.save(oldRoom);
            }

            RentalDetail newDetail = new RentalDetail();
            newDetail.setId(AppUtils.generateId("RDT", 10));
            newDetail.setRental(oldDetail.getRental());
            newDetail.setRoom(newRoom);
            newDetail.setCheckinDate(now);
            newDetail.setRentalType(rentalType);
            newDetail.setLengthOfStay(remainingHours);
            newDetail.setUnitPrice(resolvedNewPrice);
            newDetail.setAdultCount(oldDetail.getAdultCount());
            newDetail.setChildrenCount(oldDetail.getChildrenCount());
            newDetail.setStatus(RentalDetailStatus.CHECKED_IN);
            newDetail.setPreviousDetailId(oldDetail.getId());
            newDetail.setNote("Chuyển từ phòng " + oldRoomName + (isHotelFault ? " (Lỗi KS - giữ ưu đãi giá)" : " (Khách yêu cầu)") + (note.isEmpty() ? "" : ": " + note));

            if (oldDetail.getRentalGuests() != null) {
                for (RentalGuest g : oldDetail.getRentalGuests()) {
                    RentalGuest copied = new RentalGuest();
                    copied.setId(AppUtils.generateId("RGS", 10));
                    copied.setFullName(g.getFullName());
                    copied.setPid(g.getPid());
                    copied.setPhoneNumber(g.getPhoneNumber());
                    copied.setIsAdult(g.getIsAdult());
                    copied.setIsRepresentative(g.getIsRepresentative());
                    copied.setRentalDetail(newDetail);
                    newDetail.getRentalGuests().add(copied);
                }
            }
            rentalDetailRepository.save(newDetail);

            return "Đã chuyển khách từ phòng " + oldRoomName + " sang phòng " + newRoom.getName() + " thành công.";
        }

        throw new IllegalArgumentException("Trạng thái phòng hiện tại không hỗ trợ đổi phòng.");
    }

    @Transactional
    public String markRoomAvailable(String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null && room.getStatus() == RoomStatus.CLEANING) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
            return "Đã mở lại phòng " + room.getName();
        }
        throw new IllegalArgumentException("Phòng không ở trạng thái cần dọn dẹp.");
    }

    // ==================== PRIVATE HELPERS ====================

    private void sendCheckoutEmail(Rental rental) {
        if (rental.getCustomer() == null || rental.getCustomer().getUser() == null) return;
        Users u = rental.getCustomer().getUser();
        BillingSummary summary = billingService.buildBillingSummary(rental);
        double baseRoomOnly = Math.max(0.0, summary.roomAmount() - summary.earlyCheckinSurcharge() - summary.lateCheckoutSurcharge());

        Set<String> detailIds = rental.getRentalDetails().stream().map(RentalDetail::getId).collect(Collectors.toSet());
        List<ServiceUsage> usages = serviceUsageRepository.findAllByOrderByTimeDesc().stream()
                .filter(su -> su.getRentalDetail() != null && detailIds.contains(su.getRentalDetail().getId()))
                .toList();

        StringBuilder svcText = new StringBuilder();
        for (ServiceUsage su : usages) {
            String sName = su.getService() != null ? su.getService().getName() : "Dịch vụ";
            int count = su.getCount() != null ? su.getCount() : 0;
            double price = su.getUnitPrice() != null ? su.getUnitPrice() : 0.0;
            svcText.append("   + ").append(sName).append(" (SL: ").append(count).append(" x ")
                   .append(AppUtils.formatMoney(price)).append("): ").append(AppUtils.formatMoney(count * price)).append("\n");
        }

        String paymentMethodStr = paymentRepository.findByBillRentalIdOrderByDateDesc(rental.getId()).stream()
                .filter(p -> p.getBill() != null && p.getBill().getType() != BillType.DEPOSIT)
                .findFirst()
                .map(p -> p.getMethod() != null ? p.getMethod().getDisplayName() : "Tại quầy")
                .orElse("Tại quầy");

        TierHistory currentTier = customerTierService.findTierHistoryEffectiveAt(rental.getCustomer().getId(), LocalDateTime.now());
        String tierName = (currentTier != null && currentTier.getTierCustomer() != null)
                ? currentTier.getTierCustomer().getName() : "Thành viên";

        emailService.sendCheckoutThankYouEmail(
                u.getEmail(), AppUtils.buildFullName(u.getFirstName(), u.getLastName()),
                rental.getId(), rentalOperationService.extractRoomNames(rental),
                AppUtils.formatMoney(baseRoomOnly), AppUtils.formatMoney(summary.earlyCheckinSurcharge()),
                AppUtils.formatMoney(summary.lateCheckoutSurcharge()), svcText.toString(),
                AppUtils.formatMoney(summary.serviceAmount()), AppUtils.formatMoney(summary.depositPaid()),
                AppUtils.formatMoney(summary.finalAmount()), paymentMethodStr, tierName
        );
    }
}