package com.hotel.system.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{9,11}$", message = "Số điện thoại phải từ 9 đến 11 chữ số")
    private String phoneNumber;

    @NotBlank(message = "CCCD/CMND không được để trống")
    @Size(min = 9, max = 20, message = "CCCD/CMND phải từ 9 đến 20 ký tự")
    private String pid;
}