package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("Quản trị viên"),
    MANAGER("Quản lý"),
    STAFF("Nhân viên lễ tân"),
    CUSTOMER("Khách hàng");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }
}