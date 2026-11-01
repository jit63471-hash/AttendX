package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.ClassSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassSessionRepository {
    private static final Logger log = LoggerFactory.getLogger(ClassSessionRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private static final String SELECT_BASE =
            "SELECT cs.*, sub.name as subject_name, sub.code as subject_code, " +
            "CONCAT(f.first_name, ' ', f.last_name) as faculty_name " +
            "FROM class_sessions cs " +
            "LEFT JOIN subjects sub ON cs.subject_id = sub.id " +
            "LEFT JOIN faculty f ON cs.faculty_id = f.id ";

    public Optional<ClassSession> findById(int id) {
        String sql = SELECT_BASE + "WHERE cs.id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding class session by id: {}", id, e);
            throw new DatabaseException("Failed to find class session", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<ClassSession> findBySubjectAndDate(int subjectId, LocalDate date) {
        String sql = SELECT_BASE + "WHERE cs.subject_id = ? AND cs.session_date = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subjectId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding class session by subject {} and date {}", subjectId, date, e);
            throw new DatabaseException("Failed to find class session", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public int saveAndGetId(ClassSession session) {
        String sql = "INSERT INTO class_sessions (subject_id, faculty_id, session_date, topic, status) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, session.getSubjectId());
            pstmt.setInt(2, session.getFacultyId());
            pstmt.setString(3, session.getSessionDate().toString());
            pstmt.setString(4, session.getTopic());
            pstmt.setString(5, session.getStatus() != null ? session.getStatus() : "COMPLETED");

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    session.setId(rs.getInt(1));
                    return session.getId();
                }
            }
            return session.getId();
        } catch (SQLException e) {
            log.error("Error saving class session", e);
            throw new DatabaseException("Failed to save class session: " + e.getMessage(), e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public List<ClassSession> findRecent(int limit) {
        List<ClassSession> list = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY cs.session_date DESC, cs.id DESC " + (db.isMySQL() ? "LIMIT ?" : "LIMIT ?");
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding recent sessions", e);
            throw new DatabaseException("Failed to find recent sessions", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    private ClassSession mapRow(ResultSet rs) throws SQLException {
        ClassSession cs = new ClassSession();
        cs.setId(rs.getInt("id"));
        cs.setSubjectId(rs.getInt("subject_id"));
        cs.setSubjectName(rs.getString("subject_name"));
        cs.setSubjectCode(rs.getString("subject_code"));
        cs.setFacultyId(rs.getInt("faculty_id"));
        cs.setFacultyName(rs.getString("faculty_name"));
        String dateStr = rs.getString("session_date");
        if (dateStr != null && !dateStr.isEmpty()) {
            cs.setSessionDate(LocalDate.parse(dateStr.substring(0, 10)));
        }
        cs.setTopic(rs.getString("topic"));
        cs.setStatus(rs.getString("status"));
        return cs;
    }
}
