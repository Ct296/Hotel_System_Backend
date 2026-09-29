package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum RoomStatus {
    AVAILABLE("Sẵn sàng"),
    CLEANING("Đang dọn dẹp"),
    MAINTENANCE("Đang bảo trì");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }
}