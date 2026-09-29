package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum AttendanceStatus {
    NOT_STARTED("Chưa tới ca"),
    NOT_ATTENDED("Chưa chấm công"),
    CHECKED_IN("Đã check-in"),
    COMPLETED("Đã chấm công"),
    MISSED("Vắng ca");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }
}
