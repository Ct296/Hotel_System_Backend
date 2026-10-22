package com.hotel.system.dto.timeline;

import com.hotel.system.entity.Room;
import java.util.List;

public record RoomTimelineRowView(
        Room room,
        String currentStateLabel,
        String nextAvailableLabel,
        boolean availableNow,
        boolean maintenanceRoom,
        List<RoomTimelineSegmentView> segments
) {}