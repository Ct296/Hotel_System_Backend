package com.hotel.system.service;

import com.hotel.system.dto.timeline.RoomTimelineRowView;
import com.hotel.system.dto.timeline.RoomTimelineSegmentView;
import com.hotel.system.dto.timeline.TimelineSlotView;
import com.hotel.system.entity.Rental;
import com.hotel.system.entity.RentalDetail;
import com.hotel.system.entity.Room;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.RentalDetailStatus;
import com.hotel.system.entity.enums.RoomStatus;
import com.hotel.system.repository.BillRepository;
import com.hotel.system.util.AppUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class RoomTimelineService {
    private static final long CLEANING_BUFFER_MINUTES = 30;
    
    // Đã sửa sang RentalDetailStatus
    private static final Set<RentalDetailStatus> BLOCKING_BOOKING_STATUSES = Set.of(
            RentalDetailStatus.PENDING, RentalDetailStatus.CHECKED_IN, RentalDetailStatus.OVERDUE
    );

    private final BillRepository billRepository;
    private final RentalOperationService rentalOperationService;

    public RoomTimelineService(BillRepository billRepository, RentalOperationService rentalOperationService) {
        this.billRepository = billRepository;
        this.rentalOperationService = rentalOperationService;
    }

    public List<TimelineSlotView> buildRoomTimelineSlots(LocalDateTime timelineStart, int slotCount) {
        List<TimelineSlotView> slots = new ArrayList<>();
        if (timelineStart == null || slotCount <= 0) return slots;
        for (int i = 0; i < slotCount; i++) {
            LocalDateTime slotStart = timelineStart.plusMinutes(i * 30L);
            slots.add(new TimelineSlotView(slotStart, slotStart.plusMinutes(30), slotStart.format(DateTimeFormatter.ofPattern("HH:mm"))));
        }
        return slots;
    }

    public List<RoomTimelineRowView> buildRoomTimelineRows(List<Room> rooms, List<Rental> rentals, LocalDateTime timelineStart, LocalDateTime timelineEnd) {
        List<RoomTimelineRowView> rows = new ArrayList<>();
        if (rooms == null || rooms.isEmpty() || timelineStart == null || timelineEnd == null || !timelineEnd.isAfter(timelineStart)) return rows;
        for (Room room : rooms) rows.add(buildRoomTimelineRow(room, rentals, timelineStart, timelineEnd));
        return rows;
    }

    private RoomTimelineRowView buildRoomTimelineRow(Room room, List<Rental> rentals, LocalDateTime timelineStart, LocalDateTime timelineEnd) {
        List<RoomTimelineSegmentView> segments = new ArrayList<>();
        if (room == null) return new RoomTimelineRowView(null, "N/A", "N/A", false, false, segments);

        List<RentalDetail> roomBlockingDetails = rentals == null ? List.of() : rentals.stream()
                .filter(rental -> rental != null && rental.getRentalDetails() != null)
                .flatMap(rental -> rental.getRentalDetails().stream())
                .filter(detail -> detail.getRoom() != null && room.getId() != null && room.getId().equals(detail.getRoom().getId()))
                .filter(detail -> detail.getStatus() != null && BLOCKING_BOOKING_STATUSES.contains(detail.getStatus()))
                .filter(detail -> detail.getCheckinDate() != null)
                .filter(detail -> rentalOperationService.calculateDetailEndWithBuffer(detail) != null)
                .filter(detail -> detail.getCheckinDate().isBefore(timelineEnd)
                        && rentalOperationService.calculateDetailEndWithBuffer(detail).isAfter(timelineStart))
                .sorted(Comparator.comparing(RentalDetail::getCheckinDate))
                .toList();

        LocalDateTime cleaningUntil = resolveCleaningUntil(room);
        LocalDateTime cursor = timelineStart;

        while (cursor.isBefore(timelineEnd)) {
            SegmentDescriptor descriptor = resolveSegmentDescriptor(room, roomBlockingDetails, cursor, timelineEnd, cleaningUntil);
            LocalDateTime nextCursor = descriptor.end() == null || !descriptor.end().isAfter(cursor) ? cursor.plusMinutes(30) : descriptor.end();
            if (nextCursor.isAfter(timelineEnd)) nextCursor = timelineEnd;
            int colspan = (int) Math.max(1, ChronoUnit.MINUTES.between(cursor, nextCursor) / 30);
            segments.add(new RoomTimelineSegmentView(descriptor.label(), descriptor.tooltip(), descriptor.cssClass(), colspan));
            cursor = nextCursor;
        }

        LocalDateTime availableAt = resolveNextAvailableAt(room, roomBlockingDetails, cleaningUntil, timelineStart);
        boolean availableNow = availableAt == null || !availableAt.isAfter(LocalDateTime.now());
        boolean maintenanceRoom = room.getStatus() == RoomStatus.MAINTENANCE;

        return new RoomTimelineRowView(
                room,
                resolveCurrentRoomStateLabel(room, roomBlockingDetails, cleaningUntil),
                formatAvailableAtLabel(availableAt, maintenanceRoom),
                availableNow,
                maintenanceRoom,
                segments
        );
    }

    private SegmentDescriptor resolveSegmentDescriptor(Room room, List<RentalDetail> roomBlockingDetails, LocalDateTime slotStart, LocalDateTime timelineEnd, LocalDateTime cleaningUntil) {
        if (room != null && room.getStatus() == RoomStatus.MAINTENANCE) {
            return new SegmentDescriptor("Bảo trì", "Phòng đang bảo trì.", "maintenance", timelineEnd);
        }
        if (room != null && room.getStatus() == RoomStatus.CLEANING && cleaningUntil != null && slotStart.isBefore(cleaningUntil)) {
            return new SegmentDescriptor("Dọn dẹp", "Phòng đang dọn dẹp.", "cleaning", cleaningUntil);
        }

        RentalDetail blockingDetail = roomBlockingDetails.stream()
                .filter(detail -> {
                    LocalDateTime endWithBuffer = rentalOperationService.calculateDetailEndWithBuffer(detail);
                    return endWithBuffer != null && !slotStart.isBefore(detail.getCheckinDate()) && slotStart.isBefore(endWithBuffer);
                }).findFirst().orElse(null);

        if (blockingDetail != null) {
            LocalDateTime checkout = rentalOperationService.calculateDetailEndWithoutBuffer(blockingDetail);
            LocalDateTime endWithBuffer = rentalOperationService.calculateDetailEndWithBuffer(blockingDetail);
            LocalDateTime segmentEnd = endWithBuffer == null ? slotStart.plusMinutes(30) : endWithBuffer;

            if (checkout != null && !slotStart.isBefore(checkout) && slotStart.isBefore(segmentEnd)) {
                return new SegmentDescriptor("Dọn dẹp", buildCleaningTooltip(blockingDetail.getRental(), segmentEnd), "cleaning", segmentEnd);
            }
            return new SegmentDescriptor(
                    buildDetailTimelineLabel(blockingDetail),
                    buildDetailTimelineTooltip(blockingDetail, checkout, endWithBuffer),
                    mapTimelineCssClass(blockingDetail.getStatus()),
                    checkout == null ? segmentEnd : checkout
            );
        }

        RentalDetail nextDetail = roomBlockingDetails.stream()
                .filter(detail -> detail.getCheckinDate().isAfter(slotStart))
                .findFirst().orElse(null);

        LocalDateTime availableUntil = nextDetail != null ? nextDetail.getCheckinDate() : timelineEnd;
        return new SegmentDescriptor("Trống", nextDetail != null ? "Trống đến " + formatTimelineDateTime(availableUntil) : "Phòng trống.", "available", availableUntil);
    }

    private LocalDateTime resolveCleaningUntil(Room room) {
        if (room == null || room.getStatus() != RoomStatus.CLEANING || room.getId() == null) return null;
        LocalDateTime latestCheckoutTime = rentalOperationService.findLatestCheckoutTime(room.getId());
        return (latestCheckoutTime == null ? LocalDateTime.now() : latestCheckoutTime).plusMinutes(CLEANING_BUFFER_MINUTES);
    }

    private LocalDateTime resolveNextAvailableAt(Room room, List<RentalDetail> roomBlockingDetails, LocalDateTime cleaningUntil, LocalDateTime baseTime) {
        if (room == null || room.getStatus() == RoomStatus.MAINTENANCE) return null;

        LocalDateTime pointer = baseTime == null ? LocalDateTime.now() : baseTime;
        LocalDateTime now = LocalDateTime.now();
        if (pointer.isBefore(now)) pointer = now;

        if (room.getStatus() == RoomStatus.CLEANING && cleaningUntil != null && cleaningUntil.isAfter(pointer)) pointer = cleaningUntil;

        boolean advanced;
        do {
            advanced = false;
            for (RentalDetail detail : roomBlockingDetails) {
                LocalDateTime start = detail.getCheckinDate();
                LocalDateTime end = rentalOperationService.calculateDetailEndWithBuffer(detail);
                if (start != null && end != null && !pointer.isBefore(start) && pointer.isBefore(end)) {
                    pointer = end;
                    advanced = true;
                }
            }
        } while (advanced);

        return pointer;
    }

    private String resolveCurrentRoomStateLabel(Room room, List<RentalDetail> roomBlockingDetails, LocalDateTime cleaningUntil) {
        if (room == null) return "N/A";
        if (room.getStatus() == RoomStatus.MAINTENANCE) return "Đang bảo trì";
        if (room.getStatus() == RoomStatus.CLEANING && cleaningUntil != null && cleaningUntil.isAfter(LocalDateTime.now())) return "Đang dọn dẹp";

        RentalDetail activeDetail = roomBlockingDetails.stream()
                .filter(detail -> {
                    LocalDateTime end = rentalOperationService.calculateDetailEndWithBuffer(detail);
                    return end != null && !LocalDateTime.now().isBefore(detail.getCheckinDate()) && LocalDateTime.now().isBefore(end);
                }).findFirst().orElse(null);

        if (activeDetail == null) return "Trống ngay";

        LocalDateTime expectedCheckout = rentalOperationService.calculateDetailEndWithoutBuffer(activeDetail);
        if (expectedCheckout != null && !LocalDateTime.now().isBefore(expectedCheckout)) return "Đang dọn dẹp";

        return activeDetail.getStatus() != null ? activeDetail.getStatus().getDisplayName() : "Đang bận";
    }

    private String formatAvailableAtLabel(LocalDateTime availableAt, boolean maintenanceRoom) {
        if (maintenanceRoom) return "Tạm khóa";
        if (availableAt == null) return "Chưa xác định";
        if (!availableAt.isAfter(LocalDateTime.now())) return "Ngay bây giờ";
        return formatTimelineDateTime(availableAt);
    }

    private String buildDetailTimelineLabel(RentalDetail detail) {
        String roomState = detail != null && detail.getStatus() != null ? detail.getStatus().getDisplayName() : "Đang chiếm chỗ";
        if (detail == null || detail.getRental() == null) return roomState;
        String customerName = resolveCustomerName(detail.getRental());
        return "N/A".equals(customerName) ? roomState + " - " + detail.getRental().getId() : roomState + " - " + customerName;
    }

    private String buildDetailTimelineTooltip(RentalDetail detail, LocalDateTime checkout, LocalDateTime endWithBuffer) {
        if (detail == null) return "";
        Rental rental = detail.getRental();
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Mã: ").append(rental == null || rental.getId() == null ? "N/A" : rental.getId());
        tooltip.append(" | Trạng thái: ").append(detail.getStatus() != null ? detail.getStatus().getDisplayName() : "N/A");
        tooltip.append(" | Khách: ").append(resolveCustomerName(rental));
        tooltip.append(" | Từ: ").append(formatTimelineDateTime(detail.getCheckinDate()));
        tooltip.append(" | Đến: ").append(formatTimelineDateTime(checkout));
        if (endWithBuffer != null) tooltip.append(" | Chặn đến: ").append(formatTimelineDateTime(endWithBuffer));
        return tooltip.toString();
    }

    private String buildCleaningTooltip(Rental rental, LocalDateTime cleaningUntil) {
        StringBuilder tooltip = new StringBuilder("Buffer dọn dẹp");
        if (rental != null && rental.getId() != null) tooltip.append(" sau đơn ").append(rental.getId());
        if (cleaningUntil != null) tooltip.append(" tới ").append(formatTimelineDateTime(cleaningUntil));
        return tooltip.toString();
    }

    private String resolveCustomerName(Rental rental) {
        if (rental == null || rental.getCustomer() == null || rental.getCustomer().getUser() == null) return "N/A";
        Users user = rental.getCustomer().getUser();
        String fullName = (AppUtils.safeTrim(user.getLastName()) + " " + AppUtils.safeTrim(user.getFirstName())).trim();
        return fullName.isBlank() ? "N/A" : fullName;
    }

    private String mapTimelineCssClass(RentalDetailStatus status) {
        if (status == null) return "pending";
        return switch (status) {
            case PENDING -> "pending";
            case CHECKED_IN -> "checkedin";
            case OVERDUE -> "overdue";
            default -> "pending";
        };
    }


    private String formatTimelineDateTime(LocalDateTime value) {
        return value == null ? "N/A" : value.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    private record SegmentDescriptor(String label, String tooltip, String cssClass, LocalDateTime end) {}
}