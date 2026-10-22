package com.hotel.system.dto.admin;

import com.hotel.system.entity.enums.Gender;
import com.hotel.system.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminCreateAccountRequest {
    @NotBlank(message = "Họ không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Họ chỉ chứa chữ cái")
    private String lastName;

    @NotBlank(message = "Tên không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Tên chỉ chứa chữ cái")
    private String firstName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "CCCD/CMND không được để trống")
    private String pid;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{9,11}$", message = "Số điện thoại từ 9-11 số")
    private String phoneNumber;

    @NotBlank(message = "Quốc tịch không được để trống")
    private String nationality;

    @NotBlank(message = "Ngày sinh không được để trống")
    private String dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender sex;

    @NotNull(message = "Vai trò không được để trống")
    private Role role;

    private String managerType;
    private String employmentTime;
}