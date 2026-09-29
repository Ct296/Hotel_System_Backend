package com.hotel.system.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ManagerType {
    HR_MANAGER("Quản lý nhân sự"),
    ROOM_PRICING_MANAGER("Quản lý phòng và giá"),
    SERVICE_MANAGER("Quản lý dịch vụ"),
    CUSTOMER_MANAGER("Quản lý khách hàng");

    private final String displayName;
}