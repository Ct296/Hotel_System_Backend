package com.hotel.system.dto.timeline;

public record RoomTimelineSegmentView(
        String label,
        String tooltip,
        String cssClass,
        int colspan
) {}