package com.hotel.system.entity.enums;

public enum WorkingStatus {
    WORKING("Đang làm việc"),
    SUSPENDED("Nghỉ tạm thời/Đình chỉ"),
    RESIGNED("Đã nghỉ việc");

    private final String displayName;

    WorkingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}