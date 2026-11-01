package com.attendx.service;

import com.attendx.exception.AuthException;
import com.attendx.model.User;
import com.attendx.repository.UserRepository;
import com.attendx.security.PasswordHasher;
import com.attendx.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepo = new UserRepository();
    private final AuditService auditService = new AuditService();

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AuthException("Username and password are required.");
        }

        Optional<User> userOpt = userRepo.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            auditService.log("LOGIN_FAILED", "USER", null, "Username: " + username, "User not found");
            throw new AuthException("Invalid username or password.");
        }

        User user = userOpt.get();
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            auditService.log("LOGIN_FAILED", "USER", user.getId(), "Username: " + username, "Incorrect password");
            throw new AuthException("Invalid username or password.");
        }

        if (!user.isActive()) {
            auditService.log("LOGIN_BLOCKED", "USER", user.getId(), null, "Account inactive");
            throw new AuthException("This account is inactive. Please contact your administrator.");
        }

        // Set session
        SessionManager.getInstance().login(user);
        userRepo.updateLastLogin(user.getId());
        auditService.log("LOGIN_SUCCESS", "USER", user.getId(), null, "Role: " + user.getRoleName());
        log.info("User {} successfully logged in with role {}", user.getUsername(), user.getRoleName());

        return user;
    }

    public void logout() {
        if (SessionManager.getInstance().isLoggedIn()) {
            User u = SessionManager.getInstance().getCurrentUser().get();
            auditService.log("LOGOUT", "USER", u.getId(), null, "User logged out");
            log.info("User {} logged out", u.getUsername());
        }
        SessionManager.getInstance().logout();
    }
}
