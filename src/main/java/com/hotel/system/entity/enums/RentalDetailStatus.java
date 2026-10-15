package com.hotel.system.entity.enums;
import lombok.Getter;

@Getter
public enum RentalDetailStatus {
    PENDING("Chờ nhận phòng"),
    CHECKED_IN("Đã nhận phòng"),
    OVERDUE("Đã quá hạn trả phòng"),
    CHECKED_OUT("Đã rời đi"),
    CANCELLED("Đã hủy riêng");

    private final String displayName;

    RentalDetailStatus(String displayName) {
        this.displayName = displayName;
}
}
