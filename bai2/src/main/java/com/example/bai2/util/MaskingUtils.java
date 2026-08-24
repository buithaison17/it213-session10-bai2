package com.example.bai2.util;

public final class MaskingUtils {
    private MaskingUtils() {
        // Utility class
    }

    /**
     * Masking số tài khoản: ví dụ "0123456789" -> "******6789"
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int visibleCount = 4;
        int maskedCount = accountNumber.length() - visibleCount;
        return "*".repeat(maskedCount) + accountNumber.substring(maskedCount);
    }

    /**
     * Masking tên khách hàng: ví dụ "Nguyen Van A" -> "N*** A"
     */
    public static String maskUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "***";
        }
        String trimmed = username.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
    }
}
