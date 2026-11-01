package com.attendx.service;

import com.attendx.exception.ValidationException;
import com.attendx.model.Student;
import com.attendx.repository.StudentRepository;
import com.attendx.util.ExcelExporter;
import com.attendx.validation.InputValidator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class StudentService {
    private final StudentRepository studentRepo = new StudentRepository();
    private final AuditService auditService = new AuditService();

    public List<Student> getAllStudents() {
        return studentRepo.findAll();
    }

    public List<Student> getActiveStudents() {
        return studentRepo.findActive();
    }

    public Optional<Student> getStudentById(int id) {
        return studentRepo.findById(id);
    }

    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getActiveStudents();
        }
        return studentRepo.search(query);
    }

    public List<Student> getStudentsByGroup(int deptId, int courseId, int semId) {
        return studentRepo.findByGroup(deptId, courseId, semId);
    }

    public boolean createStudent(Student s) {
        validateStudent(s);
        // Check uniqueness of registration number
        Optional<Student> existing = studentRepo.findByRegistrationNumber(s.getRegistrationNumber());
        if (existing.isPresent()) {
            throw new ValidationException("Registration Number", "A student with Registration Number '" + s.getRegistrationNumber() + "' already exists.");
        }

        boolean saved = studentRepo.save(s);
        if (saved) {
            auditService.log("CREATE_STUDENT", "STUDENT", s.getId(), null, s.getRegistrationNumber() + " - " + s.getFullName());
        }
        return saved;
    }

    public boolean updateStudent(Student s) {
        validateStudent(s);
        // Check registration number uniqueness against other students
        Optional<Student> existing = studentRepo.findByRegistrationNumber(s.getRegistrationNumber());
        if (existing.isPresent() && existing.get().getId() != s.getId()) {
            throw new ValidationException("Registration Number", "Registration number is already in use by another student.");
        }

        boolean updated = studentRepo.update(s);
        if (updated) {
            auditService.log("UPDATE_STUDENT", "STUDENT", s.getId(), null, s.getRegistrationNumber() + " - " + s.getFullName());
        }
        return updated;
    }

    public boolean archiveStudent(int id) {
        boolean archived = studentRepo.setActiveStatus(id, false);
        if (archived) {
            auditService.log("ARCHIVE_STUDENT", "STUDENT", id, "Active", "Archived");
        }
        return archived;
    }

    public boolean restoreStudent(int id) {
        boolean restored = studentRepo.setActiveStatus(id, true);
        if (restored) {
            auditService.log("RESTORE_STUDENT", "STUDENT", id, "Archived", "Active");
        }
        return restored;
    }

    public int importFromExcel(File file, int defaultDeptId, int defaultCourseId, int defaultSemId, String academicYear) throws IOException {
        List<Student> students = ExcelExporter.importStudentsFromExcel(file, defaultDeptId, defaultCourseId, defaultSemId, academicYear);
        int importedCount = 0;
        for (Student s : students) {
            try {
                if (studentRepo.findByRegistrationNumber(s.getRegistrationNumber()).isEmpty()) {
                    if (studentRepo.save(s)) {
                        importedCount++;
                    }
                }
            } catch (Exception ignored) {}
        }
        auditService.log("EXCEL_IMPORT_STUDENTS", "STUDENT", null, null, "Imported " + importedCount + " students from " + file.getName());
        return importedCount;
    }

    private void validateStudent(Student s) {
        InputValidator.validateRegistrationNumber(s.getRegistrationNumber());
        InputValidator.validateNotEmpty(s.getFirstName(), "First Name");
        InputValidator.validateNotEmpty(s.getLastName(), "Last Name");
        InputValidator.validateEmail(s.getEmail(), "Email");
        InputValidator.validatePhone(s.getPhone(), "Phone");
        if (s.getDepartmentId() <= 0) {
            throw new ValidationException("Department", "Please select a department.");
        }
        if (s.getCourseId() <= 0) {
            throw new ValidationException("Course", "Please select a course.");
        }
        if (s.getSemesterId() <= 0) {
            throw new ValidationException("Semester", "Please select a semester.");
        }
    }
}
