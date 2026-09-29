package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum IncidentStatus {
    PROCESSING("Đang xử lý"),
    RESOLVED("Đã giải quyết");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }
}