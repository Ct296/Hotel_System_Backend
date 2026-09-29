package com.hotel.system.entity.enums;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public enum Shift {
    MORNING("Sáng", LocalTime.of(6, 0), LocalTime.of(12, 0)),
    AFTERNOON("Chiều", LocalTime.of(12, 0), LocalTime.of(18, 0)),
    EVENING("Tối", LocalTime.of(18, 0), LocalTime.MIDNIGHT),
    NIGHT("Khuya", LocalTime.MIDNIGHT, LocalTime.of(6, 0));

    private final String displayName;
    private final LocalTime startTime;
    private final LocalTime endTime;

    Shift(String displayName, LocalTime startTime, LocalTime endTime) {
        this.displayName = displayName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
