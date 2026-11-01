import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceModel {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    public AttendanceModel() {
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        String createStudents = "CREATE TABLE IF NOT EXISTS students ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "roll_no TEXT UNIQUE NOT NULL, "
                + "name TEXT NOT NULL);";

        String createAttendance = "CREATE TABLE IF NOT EXISTS attendance ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id INTEGER NOT NULL, "
                + "att_date TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "UNIQUE(student_id, att_date), "
                + "FOREIGN KEY(student_id) REFERENCES students(id));";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createStudents);
            stmt.execute(createAttendance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add a new student
    public boolean addStudent(String rollNo, String name) {
        String sql = "INSERT INTO students(roll_no, name) VALUES(?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rollNo.trim());
            pstmt.setString(2, name.trim());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // NEW: Delete a student completely (along with their attendance records)
    public boolean deleteStudent(int studentId) {
        String deleteAttSql = "DELETE FROM attendance WHERE student_id = ?";
        String deleteStuSql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt1 = conn.prepareStatement(deleteAttSql);
             PreparedStatement pstmt2 = conn.prepareStatement(deleteStuSql)) {
            pstmt1.setInt(1, studentId);
            pstmt1.executeUpdate();

            pstmt2.setInt(1, studentId);
            return pstmt2.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // NEW: Delete a single attendance log row selected in the table
    public boolean deleteAttendanceLog(String rollNo, String date) {
        String sql = "DELETE FROM attendance WHERE att_date = ? "
                   + "AND student_id = (SELECT id FROM students WHERE roll_no = ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.trim());
            pstmt.setString(2, rollNo.trim());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getAllStudents() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, roll_no, name FROM students ORDER BY roll_no";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getInt("id") + " | " + rs.getString("roll_no") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean markAttendance(int studentId, String date, String status) {
        String sql = "INSERT OR REPLACE INTO attendance(student_id, att_date, status) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, date.trim());
            pstmt.setString(3, status);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getDailyLogs(String date) {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT s.roll_no, s.name, a.att_date, a.status "
                + "FROM attendance a JOIN students s ON a.student_id = s.id "
                + "WHERE a.att_date = ? ORDER BY s.roll_no";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.trim());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(new String[]{
                        rs.getString("roll_no"),
                        rs.getString("name"),
                        rs.getString("att_date"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public List<String[]> getMonthlySummary(String yearMonth) {
        List<String[]> summary = new ArrayList<>();
        String sql = "SELECT s.roll_no, s.name, "
                + "COUNT(a.id) AS total_days, "
                + "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) AS present_days, "
                + "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END) AS absent_days "
                + "FROM students s "
                + "LEFT JOIN attendance a ON s.id = a.student_id AND a.att_date LIKE ? "
                + "GROUP BY s.id ORDER BY s.roll_no";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, yearMonth.trim() + "-%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int total = rs.getInt("total_days");
                int present = rs.getInt("present_days");
                int absent = rs.getInt("absent_days");
                double percentage = (total == 0) ? 0.0 : (present * 100.0) / total;
                String remark = (total == 0) ? "No Classes" : (percentage < 75.0 ? "SHORTAGE (<75%)" : "OK (>=75%)");

                summary.add(new String[]{
                        rs.getString("roll_no"),
                        rs.getString("name"),
                        String.valueOf(total),
                        String.valueOf(present),
                        String.valueOf(absent),
                        String.format("%.2f%%", percentage),
                        remark
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }
}