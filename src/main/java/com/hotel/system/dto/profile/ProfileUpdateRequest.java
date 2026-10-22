package com.hotel.system.dto.profile;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "Họ không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Họ chỉ chứa chữ cái và tối đa 30 ký tự")
    private String lastName;

    @NotBlank(message = "Tên không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]{1,30}$", message = "Tên chỉ chứa chữ cái và tối đa 30 ký tự")
    private String firstName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{9,11}$", message = "Số điện thoại phải từ 9 đến 11 chữ số")
    private String phoneNumber;

    @NotBlank(message = "CCCD/CMND không được để trống")
    @Size(min = 9, max = 20, message = "CCCD/CMND phải từ 9 đến 20 ký tự")
    private String pid;

    @NotBlank(message = "Quốc tịch không được để trống")
    @Size(max = 100, message = "Quốc tịch tối đa 100 ký tự")
    private String nationality;

    @NotBlank(message = "Giới tính không được để trống")
    private String sex;

    @NotBlank(message = "Ngày sinh không được để trống")
    private String dateOfBirth;

    private MultipartFile avatarFile;
}