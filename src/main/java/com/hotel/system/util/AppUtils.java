package com.hotel.system.util;

import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.PaymentMethod;

import java.security.SecureRandom;
import java.util.UUID;

public class AppUtils {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";

    // Tạo ID ngẫu nhiên với tiền tố và độ dài xác định
    public static String generateId(String prefix, int totalLength) {
        int randomLength = totalLength - prefix.length();
        if (randomLength <= 0) {
            throw new IllegalArgumentException("Độ dài totalLength phải lớn hơn prefix length");
        }
        String randomPart = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + randomPart.substring(0, randomLength);
    }

    // Tạo mật khẩu ngẫu nhiên với độ dài xác định
    public static String generateRandomPassword(int length) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    // Định dạng số tiền với dấu phân cách hàng nghìn và ký hiệu tiền tệ
    public static String formatMoney(double amount) {
        long rounded = Math.round(amount);
        return String.format("%,d đ", rounded).replace(',', '.');
    }

    // Định dạng phần trăm với 2 chữ số thập phân
    public static String formatPercent(double percent) {
        return String.format("%.0f%%", percent);
    }

    // Tạo mã giao dịch dựa trên phương thức thanh toán và mã giao dịch gốc
    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    // Trả về giá trị an toàn, nếu rỗng thì trả về fallback
    public static String safeText(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    // Xây dựng tên đầy đủ từ họ và tên
    public static String buildFullName(String firstName, String lastName) {
        return (safeTrim(firstName) + " " + safeTrim(lastName)).trim();
    }

    // Xây dựng tên đầy đủ từ đối tượng người dùng, nếu null thì trả về fallback
    public static String buildFullName(Users user, String fallback) {
        if (user == null) return fallback;
        String fullName = buildFullName(user.getFirstName(), user.getLastName());
        return fullName.isEmpty() ? fallback : fullName;
    }

    // Xây dựng mã giao dịch dựa trên phương thức thanh toán và mã giao dịch gốc
    public static String buildTransactionCode(PaymentMethod paymentMethod, String transactionCode) {
        String trimmed = safeTrim(transactionCode);
        if (!trimmed.isBlank()) {
            return trimmed;
        }
        String prefix = paymentMethod == PaymentMethod.CASH ? "CASH" : "BANK";
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}