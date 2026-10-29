package com.hotel.system.dto.staff;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WalkInRequest {
    private List<String> roomId;
    private LocalDateTime checkIn;   // Giờ nhận phòng thực tế (có phút lẻ)
    private Integer lengthOfStay;    // Số giờ thuê dự kiến
    private List<Integer> adultCount;
    private List<Integer> childrenCount;

    // Thông tin khách hàng vãng lai
    private String existingCustomerPid;
    private String lastName;
    private String firstName;
    private String pid;
    private String phoneNumber;
    private String email;
    private String nationality;
    private String sex;
    private String dateOfBirth;
}