package com.attendx.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    @Test
    @DisplayName("Should hash password and successfully verify with correct plain text")
    public void testHashAndVerifySuccess() {
        String password = "SecurePassword@123";
        String hashed = PasswordHasher.hash(password);

        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$"));
        assertTrue(PasswordHasher.verify(password, hashed));
    }

    @Test
    @DisplayName("Should fail verification with incorrect plain text")
    public void testVerifyFailure() {
        String password = "CorrectPassword123";
        String hashed = PasswordHasher.hash(password);

        assertFalse(PasswordHasher.verify("WrongPassword", hashed));
        assertFalse(PasswordHasher.verify("", hashed));
        assertFalse(PasswordHasher.verify(null, hashed));
    }

    @Test
    @DisplayName("Should throw exception when hashing null or empty password")
    public void testHashNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
    }
}
