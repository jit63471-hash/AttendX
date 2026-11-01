package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubjectRepository {
    private static final Logger log = LoggerFactory.getLogger(SubjectRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<Subject> findAll() {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name as course_name, sem.label as semester_label " +
                "FROM subjects s " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "LEFT JOIN semesters sem ON s.semester_id = sem.id " +
                "WHERE s.is_active = 1 ORDER BY s.code";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding subjects", e);
            throw new DatabaseException("Failed to find subjects", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Subject> findByCourseAndSemester(int courseId, int semesterId) {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name as course_name, sem.label as semester_label " +
                "FROM subjects s " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "LEFT JOIN semesters sem ON s.semester_id = sem.id " +
                "WHERE s.course_id = ? AND s.semester_id = ? AND s.is_active = 1 ORDER BY s.code";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            pstmt.setInt(2, semesterId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding subjects by course {} and sem {}", courseId, semesterId, e);
            throw new DatabaseException("Failed to find subjects", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Subject> findById(int id) {
        String sql = "SELECT s.*, c.name as course_name, sem.label as semester_label " +
                "FROM subjects s " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "LEFT JOIN semesters sem ON s.semester_id = sem.id WHERE s.id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding subject by id: {}", id, e);
            throw new DatabaseException("Failed to find subject", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public boolean save(Subject s) {
        String sql = "INSERT INTO subjects (code, name, course_id, semester_id, credits, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, s.getCode().toUpperCase().trim());
            pstmt.setString(2, s.getName().trim());
            pstmt.setInt(3, s.getCourseId());
            pstmt.setInt(4, s.getSemesterId());
            pstmt.setInt(5, s.getCredits());
            pstmt.setInt(6, s.isActive() ? 1 : 0);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving subject: {}", s.getCode(), e);
            throw new DatabaseException("Failed to save subject", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setCourseId(rs.getInt("course_id"));
        s.setCourseName(rs.getString("course_name"));
        s.setSemesterId(rs.getInt("semester_id"));
        s.setSemesterLabel(rs.getString("semester_label"));
        s.setCredits(rs.getInt("credits"));
        s.setActive(rs.getInt("is_active") == 1);
        return s;
    }
}
