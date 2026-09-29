package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum RentalDetailStatus {
    PENDING("Chưa đến nhận phòng"),
    CHECKED_IN("Đang lưu trú"),
    OVERDUE("Quá hạn trả phòng"),
    CHECKED_OUT("Đã trả phòng"),
    CANCELLED("Đã hủy phòng");

    private final String displayName;

    RentalDetailStatus(String displayName) {
        this.displayName = displayName;
    }
}