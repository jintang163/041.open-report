package com.openreport.common.utils;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;

public class PasswordUtils {

    private static final String SALT_PREFIX = "openreport";

    public static String encrypt(String password) {
        return BCrypt.hashpw(password);
    }

    public static String encrypt(String password, String salt) {
        String saltedPassword = SALT_PREFIX + salt + password;
        return DigestUtil.sha256Hex(saltedPassword);
    }

    public static Boolean matches(String password, String encodedPassword) {
        try {
            return BCrypt.checkpw(password, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static Boolean matches(String password, String salt, String encodedPassword) {
        String saltedPassword = SALT_PREFIX + salt + password;
        return DigestUtil.sha256Hex(saltedPassword).equals(encodedPassword);
    }

    public static String generateRandomPassword() {
        return generateRandomPassword(8);
    }

    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}
