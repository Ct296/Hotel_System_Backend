package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum BillType {
    DEPOSIT("Hóa đơn đặt cọc"),
    FINAL("Hóa đơn tất toán"),
    REFUND("Hóa đơn hoàn tiền");

    private final String displayName;

    BillType(String displayName) {
        this.displayName = displayName;
    }
}