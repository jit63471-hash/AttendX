package com.attendx.service;

import com.attendx.config.AppConfig;
import com.attendx.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<String[]> getDailyReport(LocalDate date) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT s.registration_number, CONCAT(s.first_name, ' ', s.last_name) as student_name, " +
                "sub.name as subject_name, a.status, a.remarks " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "JOIN subjects sub ON cs.subject_id = sub.id " +
                "WHERE cs.session_date LIKE ? " +
                "ORDER BY s.registration_number";

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString() + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString("registration_number"),
                            rs.getString("student_name"),
                            rs.getString("subject_name"),
                            rs.getString("status"),
                            rs.getString("remarks") != null ? rs.getString("remarks") : ""
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Error generating daily report", e);
        } finally {
            db.releaseConnection(conn);
        }
        return rows;
    }

    public List<String[]> getMonthlySummaryReport(int year, int month) {
        List<String[]> rows = new ArrayList<>();
        String yearMonthStr = String.format("%04d-%02d", year, month);

        String sql = "SELECT s.registration_number, CONCAT(s.first_name, ' ', s.last_name) as student_name, " +
                "d.code as dept_code, " +
                "COUNT(a.id) as total_classes, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count, " +
                "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count, " +
                "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) as late_count " +
                "FROM students s " +
                "LEFT JOIN departments d ON s.department_id = d.id " +
                "LEFT JOIN attendance a ON s.id = a.student_id " +
                "LEFT JOIN class_sessions cs ON a.class_session_id = cs.id AND cs.session_date LIKE ? " +
                "WHERE s.is_active = 1 " +
                "GROUP BY s.id, s.registration_number, s.first_name, s.last_name, d.code " +
                "ORDER BY s.registration_number";

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, yearMonthStr + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_count");
                    int absent = rs.getInt("absent_count");
                    int late = rs.getInt("late_count");
                    double pct = total > 0 ? (present * 100.0) / total : 0.0;
                    String statusRule = total == 0 ? "No Classes" : (pct < AppConfig.MIN_ATTENDANCE_PERCENT ? "SHORTAGE (<75%)" : "ELIGIBLE (>=75%)");

                    rows.add(new String[]{
                            rs.getString("registration_number"),
                            rs.getString("student_name"),
                            rs.getString("dept_code"),
                            String.valueOf(total),
                            String.valueOf(present),
                            String.valueOf(absent),
                            String.valueOf(late),
                            String.format("%.1f%%", pct),
                            statusRule
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Error generating monthly summary report", e);
        } finally {
            db.releaseConnection(conn);
        }
        return rows;
    }

    public List<String[]> getShortageReport() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT s.registration_number, CONCAT(s.first_name, ' ', s.last_name) as student_name, " +
                "s.email, s.phone, d.name as dept_name, " +
                "COUNT(a.id) as total_classes, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count " +
                "FROM students s " +
                "LEFT JOIN departments d ON s.department_id = d.id " +
                "LEFT JOIN attendance a ON s.id = a.student_id " +
                "WHERE s.is_active = 1 " +
                "GROUP BY s.id, s.registration_number, s.first_name, s.last_name, s.email, s.phone, d.name " +
                "HAVING total_classes > 0 AND (present_count * 100.0 / total_classes) < ? " +
                "ORDER BY s.registration_number";

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, AppConfig.MIN_ATTENDANCE_PERCENT);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_count");
                    double pct = (present * 100.0) / total;

                    rows.add(new String[]{
                            rs.getString("registration_number"),
                            rs.getString("student_name"),
                            rs.getString("dept_name"),
                            rs.getString("email"),
                            rs.getString("phone") != null ? rs.getString("phone") : "",
                            String.valueOf(total),
                            String.valueOf(present),
                            String.format("%.1f%%", pct),
                            "DEFICIT: " + String.format("%.1f%%", AppConfig.MIN_ATTENDANCE_PERCENT - pct)
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Error generating shortage report", e);
        } finally {
            db.releaseConnection(conn);
        }
        return rows;
    }
}
