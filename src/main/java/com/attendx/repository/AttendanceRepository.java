package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Attendance;
import com.attendx.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class AttendanceRepository {
    private static final Logger log = LoggerFactory.getLogger(AttendanceRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public boolean saveOrUpdate(Attendance att) {
        String sql;
        if (db.isMySQL()) {
            sql = "INSERT INTO attendance (class_session_id, student_id, status, remarks, marked_by) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE status = VALUES(status), remarks = VALUES(remarks), marked_by = VALUES(marked_by)";
        } else {
            sql = "INSERT OR REPLACE INTO attendance (class_session_id, student_id, status, remarks, marked_by) " +
                    "VALUES (?, ?, ?, ?, ?)";
        }

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, att.getClassSessionId());
            pstmt.setInt(2, att.getStudentId());
            pstmt.setString(3, att.getStatus().name());
            pstmt.setString(4, att.getRemarks());
            if (att.getMarkedBy() != null) {
                pstmt.setInt(5, att.getMarkedBy());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error saving attendance for student {}", att.getStudentId(), e);
            throw new DatabaseException("Failed to save attendance", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean saveAll(List<Attendance> list) {
        if (list == null || list.isEmpty()) return true;

        String sql;
        if (db.isMySQL()) {
            sql = "INSERT INTO attendance (class_session_id, student_id, status, remarks, marked_by) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE status = VALUES(status), remarks = VALUES(remarks), marked_by = VALUES(marked_by)";
        } else {
            sql = "INSERT OR REPLACE INTO attendance (class_session_id, student_id, status, remarks, marked_by) " +
                    "VALUES (?, ?, ?, ?, ?)";
        }

        Connection conn = db.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Attendance att : list) {
                    pstmt.setInt(1, att.getClassSessionId());
                    pstmt.setInt(2, att.getStudentId());
                    pstmt.setString(3, att.getStatus().name());
                    pstmt.setString(4, att.getRemarks());
                    if (att.getMarkedBy() != null) {
                        pstmt.setInt(5, att.getMarkedBy());
                    } else {
                        pstmt.setNull(5, Types.INTEGER);
                    }
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            log.error("Error in batch attendance save", e);
            throw new DatabaseException("Failed to save bulk attendance", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public List<Attendance> findBySession(int sessionId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, s.first_name, s.last_name, s.registration_number " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "WHERE a.class_session_id = ? ORDER BY s.registration_number";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setId(rs.getInt("id"));
                    att.setClassSessionId(rs.getInt("class_session_id"));
                    att.setStudentId(rs.getInt("student_id"));
                    att.setStudentName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    att.setRegistrationNumber(rs.getString("registration_number"));
                    att.setStatus(Attendance.Status.fromString(rs.getString("status")));
                    att.setRemarks(rs.getString("remarks"));
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding attendance by session: {}", sessionId, e);
            throw new DatabaseException("Failed to find session attendance", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public int countPresentToday() {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM attendance a " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "WHERE cs.session_date LIKE ? AND a.status = 'PRESENT'";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting present today", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0;
    }

    public int countAbsentToday() {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM attendance a " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "WHERE cs.session_date LIKE ? AND a.status = 'ABSENT'";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting absent today", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0;
    }

    public double getOverallAttendancePercentage() {
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as present " +
                "FROM attendance";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                int present = rs.getInt("present");
                if (total == 0) return 0.0;
                return (present * 100.0) / total;
            }
        } catch (SQLException e) {
            log.error("Error getting overall attendance percentage", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0.0;
    }

    public List<Student> getShortageStudents(double thresholdPercent) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.id, s.registration_number, s.first_name, s.last_name, s.email, " +
                "COUNT(a.id) as total_classes, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_classes " +
                "FROM students s " +
                "LEFT JOIN attendance a ON s.id = a.student_id " +
                "WHERE s.is_active = 1 " +
                "GROUP BY s.id, s.registration_number, s.first_name, s.last_name, s.email " +
                "HAVING total_classes > 0 AND (present_classes * 100.0 / total_classes) < ? " +
                "ORDER BY s.registration_number";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, thresholdPercent);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student s = new Student();
                    s.setId(rs.getInt("id"));
                    s.setRegistrationNumber(rs.getString("registration_number"));
                    s.setFirstName(rs.getString("first_name"));
                    s.setLastName(rs.getString("last_name"));
                    s.setEmail(rs.getString("email"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            log.error("Error querying shortage students", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Map<String, Object>> getWeeklyAttendance() {
        List<Map<String, Object>> result = new ArrayList<>();
        // Query last 7 days attendance
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dateStr = d.toString();
            int present = 0;
            int absent = 0;

            String sql = "SELECT " +
                    "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count, " +
                    "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count " +
                    "FROM attendance a JOIN class_sessions cs ON a.class_session_id = cs.id " +
                    "WHERE cs.session_date LIKE ?";

            Connection conn = db.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, dateStr + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        present = rs.getInt("present_count");
                        absent = rs.getInt("absent_count");
                    }
                }
            } catch (SQLException e) {
                log.error("Error getting weekly stats for date: {}", dateStr, e);
            } finally {
                db.releaseConnection(conn);
            }

            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("day", d.getDayOfWeek().name().substring(0, 3));
            dayMap.put("date", dateStr);
            dayMap.put("present", present);
            dayMap.put("absent", absent);
            result.add(dayMap);
        }
        return result;
    }
}
