package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum RentalStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    COMPLETED("Đã hoàn tất"),
    CANCELLED("Đã hủy");

    private final String displayName;

    RentalStatus(String displayName) {
        this.displayName = displayName;
    }
}