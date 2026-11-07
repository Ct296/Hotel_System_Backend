package com.hotel.system.entity.enums;

public enum AccountState {
    ACTIVE("Đang hoạt động"),
    BOOKING_BLOCKED("Cấm đặt phòng"),
    LOCKED("Đang bị khóa");

    private final String displayName;

    AccountState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
