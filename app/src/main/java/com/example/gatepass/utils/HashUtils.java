package com.example.gatepass.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String hashString(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String hashAadhaar(String rawId) {
        return hashString(rawId);
    }

    public static boolean verifyPassword(String input, String stored) {
        if (stored == null) return false;
        if (isHashed(stored)) {
            return hashString(input).equals(stored);
        }
        // old accounts before hashing was added still have plaintext passwords
        return input.equals(stored);
    }

    public static boolean isHashed(String password) {
        return password != null && password.length() == 64;
    }
}
