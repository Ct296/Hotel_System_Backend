package com.hotel.system.dto.profile;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String oldPass;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 255, message = "Mật khẩu mới phải từ 8 đến 255 ký tự")
    private String newPass;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPass;
}