package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CASH("Tiền mặt"),
    BANK("Chuyển khoản");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
}