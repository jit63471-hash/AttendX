package com.attendx.validation;

import com.attendx.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InputValidatorTest {

    @Test
    @DisplayName("Should pass valid email addresses")
    public void testValidEmail() {
        assertDoesNotThrow(() -> InputValidator.validateEmail("student@attendx.edu", "Email"));
        assertDoesNotThrow(() -> InputValidator.validateEmail("john.doe@college.ac.in", "Email"));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid email")
    public void testInvalidEmail() {
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("not-an-email", "Email"));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail("", "Email"));
        assertThrows(ValidationException.class, () -> InputValidator.validateEmail(null, "Email"));
    }

    @Test
    @DisplayName("Should validate registration number format")
    public void testRegistrationNumberValidation() {
        assertDoesNotThrow(() -> InputValidator.validateRegistrationNumber("CS2025001"));
        assertThrows(ValidationException.class, () -> InputValidator.validateRegistrationNumber(""));
        assertThrows(ValidationException.class, () -> InputValidator.validateRegistrationNumber("A"));
    }

    @Test
    @DisplayName("Should validate date format YYYY-MM-DD")
    public void testDateValidation() {
        assertDoesNotThrow(() -> InputValidator.validateDate("2026-11-01", "Date"));
        assertThrows(ValidationException.class, () -> InputValidator.validateDate("01-11-2026", "Date"));
        assertThrows(ValidationException.class, () -> InputValidator.validateDate("invalid-date", "Date"));
    }
}
