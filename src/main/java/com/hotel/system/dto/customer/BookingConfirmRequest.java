package com.hotel.system.dto.customer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingConfirmRequest {
    @NotEmpty(message = "Vui lòng chọn ít nhất một phòng.")
    private List<String> roomId;

    @NotNull(message = "Thời gian nhận phòng không hợp lệ.")
    private LocalDateTime checkIn;

    @NotNull(message = "Thời gian trả phòng không hợp lệ.")
    private LocalDateTime checkOut;

    @NotEmpty(message = "Thiếu thông tin số người lớn.")
    private List<Integer> adultCounts;

    @NotEmpty(message = "Thiếu thông tin số trẻ em.")
    private List<Integer> childrenCounts;

    private String note;
}