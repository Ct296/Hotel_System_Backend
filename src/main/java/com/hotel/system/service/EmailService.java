package com.hotel.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private void sendSafe(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email tới {}: {}", message.getTo(), e.getMessage());
        }
    }

    @Async
    public void sendNewPasswordEmail(String toEmail, String fullName, String rawPassword) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Thông tin tài khoản");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Cảm ơn bạn đã sử dụng dịch vụ tại ITHotel.\n"
                + "Tài khoản đặt phòng online lần sau.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay để bảo mật thông tin.\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    @Async
    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otpCode, int expireMinutes) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Mã xác thực đăng ký tài khoản");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Bạn vừa yêu cầu đăng ký tài khoản tại ITHotel.\n"
                + "Vui lòng sử dụng mã xác thực dưới đây để hoàn tất đăng ký:\n\n"
                + "--------------------------------\n"
                + "Mã OTP: " + otpCode + "\n"
                + "Hiệu lực: " + expireMinutes + " phút\n"
                + "--------------------------------\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    @Async
    public void sendForgotPasswordOtpEmail(String toEmail, String fullName, String otpCode, int expireMinutes) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Mã xác thực khôi phục mật khẩu");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Hệ thống nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn tại ITHotel.\n"
                + "Vui lòng sử dụng mã OTP dưới đây để tiếp tục:\n\n"
                + "--------------------------------\n"
                + "Mã OTP: " + otpCode + "\n"
                + "Hiệu lực: " + expireMinutes + " phút\n"
                + "--------------------------------\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    @Async
    public void sendCreatedAccountEmail(String toEmail, String fullName, String rawPassword, String roleDisplayName) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Tài khoản nhân sự đã được tạo");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Tài khoản " + safeText(roleDisplayName, "nhân sự") + " của bạn trên hệ thống ITHotel đã được khởi tạo thành công.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu tạm thời: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đăng nhập đầu tiên để đảm bảo an toàn thông tin.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    @Async
    public void sendAutoCreatedCustomerAccountEmail(String toEmail, String fullName, String rawPassword) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Tài khoản khách hàng của bạn đã được tạo");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Nhân viên lễ tân vừa hỗ trợ tạo tài khoản khách hàng cho bạn trên hệ thống ITHotel.\n\n"
                + "--------------------------------\n"
                + "Email đăng nhập: " + toEmail + "\n"
                + "Mật khẩu tạm thời: " + rawPassword + "\n"
                + "--------------------------------\n\n"
                + "Bạn có thể dùng tài khoản này để đăng nhập, xem lịch sử thuê phòng và đặt phòng online cho các lần tiếp theo.\n"
                + "Vui lòng đăng nhập và đổi mật khẩu để bảo mật tài khoản.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // ==================== CÁC EMAIL TỰ ĐỘNG NGHIỆP VỤ BOOKING & LƯU TRÚ ====================

    // 1. Gửi ngay khi khách vừa đặt phòng (đang chờ thanh toán cọc trong 15 phút)
    @Async
    public void sendBookingCreatedPendingDepositEmail(String toEmail, String fullName, String rentalId, String roomNames, String checkInTime, String depositAmount) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Vui lòng thanh toán cọc cho đơn đặt phòng #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Hệ thống ITHotel đã ghi nhận yêu cầu đặt phòng của quý khách. Hiện tại đơn đang ở trạng thái CHỜ THANH TOÁN CỌC.\n\n"
                + "---------------- THÔNG TIN ĐẶT PHÒNG ----------------\n"
                + "Mã đặt phòng: " + rentalId + "\n"
                + "Phòng: " + roomNames + "\n"
                + "Giờ nhận phòng dự kiến: " + checkInTime + "\n"
                + "Số tiền cọc cần thanh toán: " + depositAmount + "\n"
                + "Thời hạn giữ chỗ: 15 phút kể từ lúc đặt\n"
                + "-----------------------------------------------------\n\n"
                + "Quý khách vui lòng hoàn tất thanh toán cọc qua cổng VNPay hoặc tại quầy lễ tân trong vòng 15 phút để xác nhận giữ phòng.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 2. Gửi khi thanh toán cọc thành công (qua VNPay hoặc Tiền mặt tại quầy)
    @Async
    public void sendBookingConfirmedEmail(String toEmail, String fullName, String rentalId, String roomNames, String checkInTime, String depositAmount, String paymentMethodName) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Xác nhận đặt phòng & Thanh toán cọc thành công #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Cảm ơn quý khách đã lựa chọn ITHotel. Đơn đặt phòng của quý khách đã được thanh toán cọc và xác nhận giữ phòng thành công!\n\n"
                + "---------------- THÔNG TIN BOOKING ----------------\n"
                + "Mã đặt phòng: " + rentalId + "\n"
                + "Phòng đã đặt: " + roomNames + "\n"
                + "Thời gian nhận phòng: " + checkInTime + "\n"
                + "Tiền cọc đã thanh toán: " + depositAmount + " (" + paymentMethodName + ")\n"
                + "Trạng thái: ĐÃ XÁC NHẬN (CONFIRMED)\n"
                + "---------------------------------------------------\n\n"
                + "Lưu ý khi nhận phòng:\n"
                + "- Quý khách vui lòng xuất trình CCCD/CMND hoặc Hộ chiếu khớp với thông tin đặt phòng tại quầy lễ tân.\n"
                + "- Nếu có thay đổi lịch trình hoặc đến trễ, vui lòng liên hệ Hotline 0901 000 001 để được hỗ trợ giữ phòng.\n\n"
                + "Hẹn gặp quý khách tại ITHotel!\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 3. Nhắc nhở khi đơn PENDING đã qua 10 phút mà chưa đóng cọc (Còn 5 phút trước khi auto hủy)
    @Async
    public void sendPendingDepositReminderEmail(String toEmail, String fullName, String rentalId, String roomNames) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - [Khẩn] Đơn đặt phòng #" + rentalId + " sắp hết hạn giữ chỗ (còn 5 phút)");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Đơn đặt phòng #" + rentalId + " (Phòng: " + roomNames + ") của quý khách hiện vẫn chưa hoàn tất thanh toán tiền cọc.\n"
                + "Hệ thống chỉ giữ phòng tối đa 15 phút. Quý khách chỉ còn khoảng 5 phút để hoàn tất thanh toán trước khi hệ thống tự động hủy đơn.\n\n"
                + "Lưu ý: Việc để đơn hủy quá 3 lần trong vòng 24 giờ sẽ khiến tài khoản bị tạm khóa tính năng đặt phòng trong 1 ngày.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 4. Thông báo khi đơn bị hủy (Khách tự hủy / Lễ tân hủy / Hệ thống tự động hủy quá hạn)
    @Async
    public void sendBookingCancelledEmail(String toEmail, String fullName, String rentalId, String roomNames, String reason) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Thông báo hủy đơn đặt phòng #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Đơn đặt phòng #" + rentalId + " (Phòng: " + roomNames + ") của quý khách đã chuyển sang trạng thái ĐÃ HỦY.\n"
                + "Chi tiết / Lý do: " + reason + "\n\n"
                + "Nếu quý khách vẫn có nhu cầu lưu trú, vui lòng truy cập lại website ITHotel để đặt phòng mới hoặc liên hệ Hotline 0901 000 001.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 5. Cảnh báo khi khách bị khóa tính năng đặt phòng 24h do spam hủy >= 3 lần
    @Async
    public void sendSpamCancelPenaltyEmail(String toEmail, String fullName, String unlockTimeStr) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Thông báo tạm khóa tính năng đặt phòng trực tuyến");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Hệ thống ghi nhận tài khoản của quý khách đã có từ 3 đơn đặt phòng bị hủy liên tiếp trong vòng 24 giờ qua.\n"
                + "Theo chính sách chống giữ phòng ảo của ITHotel, tính năng đặt phòng trực tuyến trên tài khoản của quý khách tạm thời bị khóa.\n\n"
                + "---------------------------------------------------\n"
                + "Trạng thái: Tạm cấm đặt phòng (BOOKING_BLOCKED)\n"
                + "Thời gian mở khóa tự động: " + unlockTimeStr + "\n"
                + "---------------------------------------------------\n\n"
                + "Nếu quý khách gặp sự cố trong quá trình thanh toán và cần mở khóa sớm, vui lòng liên hệ trực tiếp Hotline 0901 000 001.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 6. Nhắc lịch khi khách có đơn sắp tới giờ Check-in (còn <= 2 tiếng)
    @Async
    public void sendUpcomingCheckinReminderEmail(String toEmail, String fullName, String rentalId, String roomNames, String checkInTime) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Nhắc lịch nhận phòng sắp tới #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "ITHotel rất hân hạnh được đón tiếp quý khách! Chúng tôi xin gửi thư nhắc lịch nhận phòng sắp tới của quý khách:\n\n"
                + "---------------------------------------------------\n"
                + "Mã đặt phòng: " + rentalId + "\n"
                + "Phòng: " + roomNames + "\n"
                + "Giờ nhận phòng dự kiến: " + checkInTime + "\n"
                + "Địa chỉ: Ninh Kiều, Cần Thơ, Việt Nam\n"
                + "---------------------------------------------------\n\n"
                + "Quý khách vui lòng chuẩn bị sẵn CCCD/CMND hoặc Hộ chiếu để lễ tân hỗ trợ làm thủ tục Check-in nhanh chóng nhất.\n"
                + "Chúc quý khách có một chuyến đi thuận lợi và an toàn!\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 7. Cảnh báo khi khách đã quá giờ Check-in (15 phút) mà chưa vào -> Chuẩn bị auto hủy booking
    @Async
    public void sendOverdueCheckinWarningEmail(String toEmail, String fullName, String rentalId, String roomNames, String checkInTime) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - [Cảnh báo khẩn] Quý khách đã quá giờ nhận phòng đơn #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Theo lịch hẹn, thời gian nhận phòng cho đơn #" + rentalId + " (Phòng: " + roomNames + ") của quý khách là lúc " + checkInTime + ".\n"
                + "Hiện tại đã quá giờ nhận phòng nhưng quầy lễ tân vẫn chưa ghi nhận quý khách đến làm thủ tục Check-in.\n\n"
                + "CẢNH BÁO QUAN TRỌNG:\n"
                + "- Hệ thống đang chuẩn bị TỰ ĐỘNG HỦY BOOKING (No-show) nếu quý khách vẫn không có mặt sau 60 phút kể từ giờ hẹn nhận phòng.\n"
                + "- Theo điều khoản dịch vụ, tiền cọc sẽ không được hoàn trả khi đơn bị hủy do quá giờ nhận phòng.\n\n"
                + "Nếu quý khách đang trên đường đến hoặc gặp sự cố kẹt xe/trễ chuyến, vui lòng gọi ngay cho Lễ tân qua Hotline 0901 000 001 để được hỗ trợ giữ phòng!\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 8. Chào mừng khách khi vừa làm thủ tục Check-in nhận phòng tại quầy
    @Async
    public void sendCheckInWelcomeEmail(String toEmail, String fullName, String rentalId, String roomNames, String expectedCheckoutTime) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Chào mừng quý khách nhận phòng! (#" + rentalId + ")");

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Chào mừng quý khách đã nhận phòng tại ITHotel! Thủ tục Check-in cho đơn #" + rentalId + " đã hoàn tất.\n\n"
                + "---------------- THÔNG TIN LƯU TRÚ ----------------\n"
                + "Phòng lưu trú: " + roomNames + "\n"
                + "Thời gian trả phòng dự kiến: " + expectedCheckoutTime + "\n"
                + "Tổng đài nội bộ (Gọi lễ tân / Đặt dịch vụ): Nhấn phím 0\n"
                + "Hotline hỗ trợ 24/7: 0901 000 001\n"
                + "---------------------------------------------------\n\n"
                + "Chúc quý khách có những giây phút nghỉ ngơi thật thoải mái và trọn vẹn tại ITHotel!\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";

        message.setText(content);
        sendSafe(message);
    }

    // 9. Thư cảm ơn & Hóa đơn tất toán chi tiết khi hoàn thành Check-out
    @Async
    public void sendCheckoutThankYouEmail(String toEmail, String fullName, String rentalId, String roomNames,
                                          String roomBaseAmount, String earlySurcharge, String lateSurcharge,
                                          String serviceDetailsText, String serviceTotalAmount,
                                          String depositPaid, String finalPaidAmount, String paymentMethodName, String currentTierName) {
        if (!isValidEmail(toEmail)) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setSubject("ITHotel - Cảm ơn quý khách & Hóa đơn tất toán #" + rentalId);

        String content = "Xin chào " + safeName(fullName) + ",\n\n"
                + "Thủ tục trả phòng (Check-out) cho đơn #" + rentalId + " (Phòng: " + roomNames + ") của quý khách tại ITHotel đã hoàn tất.\n\n"
                + "================ HÓA ĐƠN TẤT TOÁN ================\n"
                + "1. Tiền phòng gốc: " + roomBaseAmount + "\n"
                + "   - Phụ thu nhận phòng sớm: " + earlySurcharge + "\n"
                + "   - Phụ thu trả phòng muộn: " + lateSurcharge + "\n"
                + "2. Dịch vụ sử dụng: " + serviceTotalAmount + "\n"
                + (serviceDetailsText != null && !serviceDetailsText.isBlank() ? serviceDetailsText : "")
                + "--------------------------------------------------\n"
                + "3. Tiền cọc đã thanh toán: -" + depositPaid + "\n"
                + "4. THANH TOÁN CUỐI KHI TRẢ PHÒNG: " + finalPaidAmount + " (" + safeText(paymentMethodName, "Tại quầy") + ")\n"
                + "--------------------------------------------------\n"
                + "Hạng thành viên hiện tại: " + safeText(currentTierName, "Thành viên") + "\n"
                + "==================================================\n\n"
                + "Thay mặt toàn thể ban quản lý và nhân viên ITHotel, chúng tôi xin gửi lời cảm ơn chân thành nhất tới quý khách vì đã tin tưởng lựa chọn ITHotel làm điểm dừng chân.\n"
                + "Kính chúc quý khách thượng lộ bình an, luôn dồi dào sức khỏe, tràn đầy năng lượng và gặt hái thật nhiều thành công trong công việc cũng như cuộc sống!\n\n"
                + "Sự hài lòng của quý khách là niềm vinh hạnh của chúng tôi. Quý khách có thể dành ít phút chia sẻ đánh giá tại https://ithotel.ct296.id.vn/reviews để ITHotel ngày càng hoàn thiện hơn nhé.\n\n"
                + "Rất mong sớm được đón tiếp quý khách trong những kỳ nghỉ tiếp theo!\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ ITHotel";
        message.setText(content);
        sendSafe(message);
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@");
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