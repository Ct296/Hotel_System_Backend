package com.hotel.system.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Họ không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Họ chỉ chứa chữ cái và tối đa 30 ký tự")
    private String lastName;

    @NotBlank(message = "Tên không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Tên chỉ chứa chữ cái và tối đa 30 ký tự")
    private String firstName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 50, message = "Email tối đa 50 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 255, message = "Mật khẩu phải từ 8 đến 255 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "CCCD/CMND không được để trống")
    @Size(min = 9, max = 20, message = "CCCD/CMND phải từ 9 đến 20 ký tự")
    private String pid;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{9,11}$", message = "Số điện thoại phải từ 9 đến 11 chữ số")
    private String phoneNumber;

    @NotBlank(message = "Quốc tịch không được để trống")
    @Size(max = 100, message = "Quốc tịch tối đa 100 ký tự")
    private String nationality;

    @NotBlank(message = "Giới tính không được để trống")
    private String sex;

    @NotBlank(message = "Ngày sinh không được để trống")
    private String dateOfBirth;
}