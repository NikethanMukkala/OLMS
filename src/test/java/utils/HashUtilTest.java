package utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HashUtilTest {
    
    @Test
    public void testPasswordHashing() {
        String password = "SecretPassword123!";
        String hash = HashUtil.hashPassword(password);
        
        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(HashUtil.checkPassword(password, hash));
        assertFalse(HashUtil.checkPassword("WrongPassword123!", hash));
    }

    @Test
    public void testDifferentSaltsProduceDifferentHashes() {
        String password = "ConsistentPassword";
        String hash1 = HashUtil.hashPassword(password);
        String hash2 = HashUtil.hashPassword(password);

        // BCrypt includes a random salt per generation, so the hashes should never strictly equate
        assertNotEquals(hash1, hash2);
        
        // However, both should successfully validate
        assertTrue(HashUtil.checkPassword(password, hash1));
        assertTrue(HashUtil.checkPassword(password, hash2));
    }
}
