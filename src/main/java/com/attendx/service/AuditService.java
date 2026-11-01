package com.attendx.service;

import com.attendx.model.AuditLog;
import com.attendx.model.User;
import com.attendx.repository.AuditLogRepository;
import com.attendx.security.SessionManager;

import java.util.List;

public class AuditService {
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public void log(String action, String entityType, Integer entityId, String oldValues, String newValues) {
        Integer userId = null;
        String username = "System";
        if (SessionManager.getInstance().isLoggedIn()) {
            User u = SessionManager.getInstance().getCurrentUser().get();
            userId = u.getId();
            username = u.getUsername();
        }

        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setIpAddress("127.0.0.1");

        auditRepo.save(log);
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditRepo.findRecent(limit);
    }
}
