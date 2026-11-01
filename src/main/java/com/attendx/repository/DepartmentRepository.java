package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentRepository {
    private static final Logger log = LoggerFactory.getLogger(DepartmentRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<Department> findAll() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT * FROM departments ORDER BY name";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error retrieving departments", e);
            throw new DatabaseException("Failed to retrieve departments", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Department> findAllActive() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT * FROM departments WHERE is_active = 1 ORDER BY name";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error retrieving active departments", e);
            throw new DatabaseException("Failed to retrieve departments", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Department> findById(int id) {
        String sql = "SELECT * FROM departments WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding department by id: {}", id, e);
            throw new DatabaseException("Failed to find department", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public boolean save(Department dept) {
        String sql = "INSERT INTO departments (code, name, description, is_active) VALUES (?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, dept.getCode().toUpperCase().trim());
            pstmt.setString(2, dept.getName().trim());
            pstmt.setString(3, dept.getDescription());
            pstmt.setInt(4, dept.isActive() ? 1 : 0);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) dept.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving department: {}", dept.getCode(), e);
            throw new DatabaseException("Failed to save department", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean update(Department dept) {
        String sql = "UPDATE departments SET code = ?, name = ?, description = ?, is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dept.getCode().toUpperCase().trim());
            pstmt.setString(2, dept.getName().trim());
            pstmt.setString(3, dept.getDescription());
            pstmt.setInt(4, dept.isActive() ? 1 : 0);
            pstmt.setInt(5, dept.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating department: {}", dept.getId(), e);
            throw new DatabaseException("Failed to update department", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM departments WHERE is_active = 1";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error counting departments", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0;
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getInt("id"));
        d.setCode(rs.getString("code"));
        d.setName(rs.getString("name"));
        d.setDescription(rs.getString("description"));
        d.setActive(rs.getInt("is_active") == 1);
        try {
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) d.setCreatedAt(ts.toLocalDateTime());
        } catch (SQLException ignored) {
            // Column may not be present in all DB configurations
        }
        return d;
    }
}
