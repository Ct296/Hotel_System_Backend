package com.hotel.system.util;

import com.hotel.system.entity.enums.PaymentMethod;

import java.security.SecureRandom;
import java.util.UUID;

public class AppUtils {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";

    public static String generateId(String prefix, int totalLength) {
        int randomLength = totalLength - prefix.length();
        if (randomLength <= 0) {
            throw new IllegalArgumentException("Độ dài totalLength phải lớn hơn prefix length");
        }
        String randomPart = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + randomPart.substring(0, randomLength);
    }

    public static String generateRandomPassword(int length) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    public static String formatMoney(double amount) {
        long rounded = Math.round(amount);
        return String.format("%,d đ", rounded).replace(',', '.');
    }

    public static String formatPercent(double percent) {
        return String.format("%.0f%%", percent);
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static String buildFullName(String firstName, String lastName) {
        return (safeTrim(lastName) + " " + safeTrim(firstName)).trim();
    }

    public static String buildTransactionCode(PaymentMethod paymentMethod, String transactionCode) {
        String trimmed = safeTrim(transactionCode);
        if (!trimmed.isBlank()) {
            return trimmed;
        }
        String prefix = paymentMethod == PaymentMethod.CASH ? "CASH" : "BANK";
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}