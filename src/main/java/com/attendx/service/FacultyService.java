package com.attendx.service;

import com.attendx.exception.ValidationException;
import com.attendx.model.Faculty;
import com.attendx.model.User;
import com.attendx.repository.FacultyRepository;
import com.attendx.repository.UserRepository;
import com.attendx.security.PasswordHasher;
import com.attendx.validation.InputValidator;

import java.util.List;
import java.util.Optional;

public class FacultyService {
    private final FacultyRepository facultyRepo = new FacultyRepository();
    private final UserRepository userRepo = new UserRepository();
    private final AuditService auditService = new AuditService();

    public List<Faculty> getAllFaculty() {
        return facultyRepo.findAll();
    }

    public List<Faculty> getActiveFaculty() {
        return facultyRepo.findActive();
    }

    public Optional<Faculty> getFacultyById(int id) {
        return facultyRepo.findById(id);
    }

    public boolean createFaculty(Faculty f, String initialPassword) {
        validateFaculty(f);
        if (initialPassword == null || initialPassword.length() < 6) {
            throw new ValidationException("Password", "Initial password must be at least 6 characters.");
        }

        // Create user account first for faculty login
        String username = f.getEmployeeId().toLowerCase().trim();
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordHasher.hash(initialPassword));
        u.setFullName(f.getFullName());
        u.setEmail(f.getEmail());
        u.setRoleId(2); // Faculty role
        u.setActive(true);

        int userId = userRepo.save(u);
        f.setUserId(userId);

        boolean saved = facultyRepo.save(f);
        if (saved) {
            auditService.log("CREATE_FACULTY", "FACULTY", f.getId(), null, f.getEmployeeId() + " - " + f.getFullName());
        }
        return saved;
    }

    public boolean updateFaculty(Faculty f) {
        validateFaculty(f);
        boolean updated = facultyRepo.update(f);
        if (updated) {
            auditService.log("UPDATE_FACULTY", "FACULTY", f.getId(), null, f.getEmployeeId() + " - " + f.getFullName());
        }
        return updated;
    }

    public boolean deactivateFaculty(int id) {
        boolean updated = facultyRepo.setActiveStatus(id, false);
        if (updated) {
            auditService.log("DEACTIVATE_FACULTY", "FACULTY", id, "Active", "Inactive");
        }
        return updated;
    }

    public boolean activateFaculty(int id) {
        boolean updated = facultyRepo.setActiveStatus(id, true);
        if (updated) {
            auditService.log("ACTIVATE_FACULTY", "FACULTY", id, "Inactive", "Active");
        }
        return updated;
    }

    private void validateFaculty(Faculty f) {
        InputValidator.validateNotEmpty(f.getEmployeeId(), "Employee ID");
        InputValidator.validateNotEmpty(f.getFirstName(), "First Name");
        InputValidator.validateNotEmpty(f.getLastName(), "Last Name");
        InputValidator.validateEmail(f.getEmail(), "Email");
        InputValidator.validatePhone(f.getPhone(), "Phone");
        if (f.getDepartmentId() <= 0) {
            throw new ValidationException("Department", "Please select a department.");
        }
    }
}
