package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {
    private static final Logger log = LoggerFactory.getLogger(AuditLogRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public boolean save(AuditLog audit) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values, ip_address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (audit.getUserId() != null) {
                pstmt.setInt(1, audit.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setString(2, audit.getAction());
            pstmt.setString(3, audit.getEntityType());
            if (audit.getEntityId() != null) {
                pstmt.setInt(4, audit.getEntityId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setString(5, audit.getOldValues());
            pstmt.setString(6, audit.getNewValues());
            pstmt.setString(7, audit.getIpAddress());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Failed to write audit log: {}", audit.getAction(), e);
            return false;
        } finally {
            db.releaseConnection(conn);
        }
    }

    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM audit_logs a " +
                "LEFT JOIN users u ON a.user_id = u.id " +
                "ORDER BY a.created_at DESC, a.id DESC " + (db.isMySQL() ? "LIMIT ?" : "LIMIT ?");
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog al = new AuditLog();
                    al.setId(rs.getInt("id"));
                    int uid = rs.getInt("user_id");
                    if (!rs.wasNull()) al.setUserId(uid);
                    al.setUsername(rs.getString("username"));
                    al.setAction(rs.getString("action"));
                    al.setEntityType(rs.getString("entity_type"));
                    int eid = rs.getInt("entity_id");
                    if (!rs.wasNull()) al.setEntityId(eid);
                    al.setOldValues(rs.getString("old_values"));
                    al.setNewValues(rs.getString("new_values"));
                    al.setIpAddress(rs.getString("ip_address"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) al.setCreatedAt(ts.toLocalDateTime());
                    list.add(al);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query recent audit logs", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }
}
