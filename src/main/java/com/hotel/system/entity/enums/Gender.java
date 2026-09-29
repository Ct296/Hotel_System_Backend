package com.hotel.system.entity.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ"),
    UNSPECIFIED("Không tiết lộ");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }
}