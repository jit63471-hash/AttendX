package com.attendx.security;

import com.attendx.model.User;

import java.util.Optional;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRoleName());
    }

    public boolean isFaculty() {
        return currentUser != null && "faculty".equalsIgnoreCase(currentUser.getRoleName());
    }

    public boolean hasPermission(String permission) {
        if (!isLoggedIn()) return false;
        if (isAdmin()) return true;
        // Faculty specific permissions
        if ("MARK_ATTENDANCE".equalsIgnoreCase(permission) || "VIEW_REPORTS".equalsIgnoreCase(permission)) {
            return true;
        }
        return false;
    }
}
