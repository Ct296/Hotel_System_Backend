package com.hotel.system.dto.staff;

import lombok.Data;
import java.util.List;

@Data
public class CheckInRoomGuestsRequest {
    private List<RoomDetailGuestGroup> roomGuests;

    @Data
    public static class RoomDetailGuestGroup {
        private String rentalDetailId;
        private List<GuestItem> guests;
    }

    @Data
    public static class GuestItem {
        private String fullName;
        private String pid;
        private String phoneNumber;
        private Boolean isAdult = true;
        private Boolean isRepresentative = false;
    }
}