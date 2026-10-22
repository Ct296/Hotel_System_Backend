package com.hotel.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendNewPasswordEmail(String toEmail, String fullName, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("ITHotel - Thông tin tài khoản của bạn");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Cảm ơn bạn đã sử dụng dịch vụ tại ITHotel.\n"
                + "Tài khoản của bạn đã được tạo tự động để đặt phòng online lần sau.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay để bảo mật thông tin.\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        mailSender.send(message);
    }

    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otpCode, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("ITHotel - Mã xác thực đăng ký tài khoản");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Bạn vừa yêu cầu đăng ký tài khoản tại ITHotel.\n"
                + "Vui lòng sử dụng mã xác thực bên dưới để hoàn tất đăng ký:\n\n"
                + "--------------------------------\n"
                + "Mã OTP: " + otpCode + "\n"
                + "Hiệu lực: " + expireMinutes + " phút\n"
                + "--------------------------------\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        mailSender.send(message);
    }

    public void sendCreatedAccountEmail(String toEmail, String fullName, String rawPassword, String roleDisplayName) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("ITHotel - Tài khoản nhân sự đã được tạo");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Tài khoản " + safeText(roleDisplayName, "nhân sự") + " của bạn trên hệ thống ITHotel đã được tạo thành công.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu tạm thời: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đăng nhập đầu tiên để đảm bảo an toàn thông tin.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        mailSender.send(message);
    }

    public void sendAutoCreatedCustomerAccountEmail(String toEmail, String fullName, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("ITHotel - Tài khoản khách hàng của bạn đã được tạo");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Nhân viên lễ tân đã tạo tài khoản khách hàng cho bạn trên hệ thống ITHotel.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu tạm thời: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Bạn có thể dùng tài khoản này để đăng nhập, xem lịch sử thuê phòng và đặt phòng online cho các lần tiếp theo.\n"
                + "Vui lòng đăng nhập và đổi mật khẩu sớm để bảo mật tài khoản.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        mailSender.send(message);
    }

    private String safeName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "quý khách";
        }
        return fullName.trim();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}