package com.hotel.system.entity.enums;

public enum ServiceStatus {
    ACTIVE("Đang hoạt động"),
    SUSPENDED("Tạm ngưng");

    private final String displayName;

    ServiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}