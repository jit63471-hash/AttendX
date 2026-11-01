package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseRepository {
    private static final Logger log = LoggerFactory.getLogger(CourseRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<Course> findAll() {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT c.*, d.name as department_name FROM courses c " +
                "LEFT JOIN departments d ON c.department_id = d.id WHERE c.is_active = 1 ORDER BY c.name";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all courses", e);
            throw new DatabaseException("Failed to find courses", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Course> findByDepartmentId(int departmentId) {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT c.*, d.name as department_name FROM courses c " +
                "LEFT JOIN departments d ON c.department_id = d.id " +
                "WHERE c.department_id = ? AND c.is_active = 1 ORDER BY c.name";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, departmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding courses by department: {}", departmentId, e);
            throw new DatabaseException("Failed to find courses by department", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Course> findById(int id) {
        String sql = "SELECT c.*, d.name as department_name FROM courses c " +
                "LEFT JOIN departments d ON c.department_id = d.id WHERE c.id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding course by id: {}", id, e);
            throw new DatabaseException("Failed to find course", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public boolean save(Course c) {
        String sql = "INSERT INTO courses (code, name, department_id, duration_semesters, is_active) VALUES (?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, c.getCode().toUpperCase().trim());
            pstmt.setString(2, c.getName().trim());
            pstmt.setInt(3, c.getDepartmentId());
            pstmt.setInt(4, c.getDurationSemesters());
            pstmt.setInt(5, c.isActive() ? 1 : 0);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) c.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving course: {}", c.getCode(), e);
            throw new DatabaseException("Failed to save course", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setDepartmentId(rs.getInt("department_id"));
        c.setDepartmentName(rs.getString("department_name"));
        c.setDurationSemesters(rs.getInt("duration_semesters"));
        c.setActive(rs.getInt("is_active") == 1);
        return c;
    }
}
