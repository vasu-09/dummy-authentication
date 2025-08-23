package com.om.backend.util;

public class PhoneNumberUtil {
    /** Assumes India. Converts 10-digit or +91 formats to E.164 (+91XXXXXXXXXX). */
    public static String toE164India(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return "+" + digits;
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        if (raw.startsWith("+") && raw.length() >= 12) {
            return raw;
        }
        throw new IllegalArgumentException("Invalid Indian mobile number: " + raw);
    }
}
