package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum RentalType {
    HOURLY("Thuê theo giờ"),
    DAILY("Thuê qua đêm / Theo ngày");

    private final String displayName;

    RentalType(String displayName) {
        this.displayName = displayName;
    }
}