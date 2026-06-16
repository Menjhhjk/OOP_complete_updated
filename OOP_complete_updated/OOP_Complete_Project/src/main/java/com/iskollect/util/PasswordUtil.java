package com.iskollect.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    //for hashing password
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    //for checking password
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.matches("^\\$2[aby]\\$.*")) {
            System.err.println("[PasswordUtil] Error: Stored password is not a valid BCrypt hash.");
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
