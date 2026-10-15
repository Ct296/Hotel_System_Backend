package com.hotel.system.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ManagerType {
    HR_MANAGER("Quản lý nhân sự"),
    OPERATION_MANAGER("Quản lý vận hành"),
    CUSTOMER_MANAGER("Quản lý khách hàng");

    private final String displayName;
}