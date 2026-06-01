package utils;

import org.mindrot.jbcrypt.BCrypt;

public class HashUtil {
    
    // Generate a strong BCrypt hash
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    // Verify a BCrypt hash
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Logically handle legacy/malformed hashes
            return false;
        }
    }
}
