package com.hotel.system.dto.timeline;

import java.time.LocalDateTime;

public record TimelineSlotView(
        LocalDateTime start,
        LocalDateTime end,
        String label
) {}