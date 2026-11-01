package com.attendx.validation;

import com.attendx.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");

    public static void validateNotEmpty(String val, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new ValidationException(fieldName, fieldName + " cannot be empty.");
        }
    }

    public static void validateEmail(String email, String fieldName) {
        validateNotEmpty(email, fieldName);
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException(fieldName, "Invalid email address format.");
        }
    }

    public static void validatePhone(String phone, String fieldName) {
        if (phone != null && !phone.trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
                throw new ValidationException(fieldName, "Phone number must contain between 7 and 15 digits.");
            }
        }
    }

    public static void validateRegistrationNumber(String regNo) {
        validateNotEmpty(regNo, "Registration Number");
        if (regNo.trim().length() < 3 || regNo.trim().length() > 50) {
            throw new ValidationException("Registration Number", "Registration number must be between 3 and 50 characters.");
        }
    }

    public static void validateDate(String dateStr, String fieldName) {
        validateNotEmpty(dateStr, fieldName);
        try {
            LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName, "Date must be in YYYY-MM-DD format.");
        }
    }

    private InputValidator() {}
}
