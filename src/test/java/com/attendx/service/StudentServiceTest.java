package com.attendx.service;

import com.attendx.exception.ValidationException;
import com.attendx.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StudentServiceTest {

    private final StudentService studentService = new StudentService();

    @Test
    @DisplayName("Should reject student with missing required fields")
    public void testCreateStudentValidationFailure() {
        Student invalidStudent = new Student();
        invalidStudent.setRegistrationNumber(""); // empty regNo
        invalidStudent.setFirstName("Test");

        assertThrows(ValidationException.class, () -> studentService.createStudent(invalidStudent));
    }

    @Test
    @DisplayName("Should validate student email during registration")
    public void testStudentEmailValidation() {
        Student student = new Student();
        student.setRegistrationNumber("TEST2025001");
        student.setFirstName("Valid");
        student.setLastName("Student");
        student.setEmail("invalid-email-address");
        student.setDepartmentId(1);
        student.setCourseId(1);
        student.setSemesterId(1);

        assertThrows(ValidationException.class, () -> studentService.createStudent(student));
    }
}
