package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Faculty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FacultyRepository {
    private static final Logger log = LoggerFactory.getLogger(FacultyRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private static final String SELECT_BASE =
            "SELECT f.*, d.name as department_name FROM faculty f " +
            "LEFT JOIN departments d ON f.department_id = d.id ";

    public List<Faculty> findAll() {
        List<Faculty> list = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY f.first_name";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all faculty", e);
            throw new DatabaseException("Failed to find faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Faculty> findActive() {
        List<Faculty> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE f.is_active = 1 ORDER BY f.first_name";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding active faculty", e);
            throw new DatabaseException("Failed to find active faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Faculty> findById(int id) {
        String sql = SELECT_BASE + "WHERE f.id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding faculty by id: {}", id, e);
            throw new DatabaseException("Failed to find faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<Faculty> findByUserId(int userId) {
        String sql = SELECT_BASE + "WHERE f.user_id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding faculty by user_id: {}", userId, e);
            throw new DatabaseException("Failed to find faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public boolean save(Faculty f) {
        String sql = "INSERT INTO faculty (employee_id, user_id, first_name, last_name, email, phone, " +
                "department_id, designation, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, f.getEmployeeId().trim());
            pstmt.setInt(2, f.getUserId());
            pstmt.setString(3, f.getFirstName().trim());
            pstmt.setString(4, f.getLastName().trim());
            pstmt.setString(5, f.getEmail().trim());
            pstmt.setString(6, f.getPhone());
            pstmt.setInt(7, f.getDepartmentId());
            pstmt.setString(8, f.getDesignation());
            pstmt.setInt(9, f.isActive() ? 1 : 0);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) f.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving faculty: {}", f.getEmployeeId(), e);
            throw new DatabaseException("Failed to save faculty: " + e.getMessage(), e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean update(Faculty f) {
        String sql = "UPDATE faculty SET employee_id = ?, first_name = ?, last_name = ?, email = ?, phone = ?, " +
                "department_id = ?, designation = ?, is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, f.getEmployeeId().trim());
            pstmt.setString(2, f.getFirstName().trim());
            pstmt.setString(3, f.getLastName().trim());
            pstmt.setString(4, f.getEmail().trim());
            pstmt.setString(5, f.getPhone());
            pstmt.setInt(6, f.getDepartmentId());
            pstmt.setString(7, f.getDesignation());
            pstmt.setInt(8, f.isActive() ? 1 : 0);
            pstmt.setInt(9, f.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating faculty: {}", f.getId(), e);
            throw new DatabaseException("Failed to update faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean setActiveStatus(int id, boolean active) {
        String sql = "UPDATE faculty SET is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating faculty active status", e);
            throw new DatabaseException("Failed to update faculty status", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM faculty WHERE is_active = 1";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error counting faculty", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0;
    }

    private Faculty mapRow(ResultSet rs) throws SQLException {
        Faculty f = new Faculty();
        f.setId(rs.getInt("id"));
        f.setEmployeeId(rs.getString("employee_id"));
        f.setUserId(rs.getInt("user_id"));
        f.setFirstName(rs.getString("first_name"));
        f.setLastName(rs.getString("last_name"));
        f.setEmail(rs.getString("email"));
        f.setPhone(rs.getString("phone"));
        f.setDepartmentId(rs.getInt("department_id"));
        f.setDepartmentName(rs.getString("department_name"));
        f.setDesignation(rs.getString("designation"));
        f.setActive(rs.getInt("is_active") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) f.setCreatedAt(ts.toLocalDateTime());
        return f;
    }
}
