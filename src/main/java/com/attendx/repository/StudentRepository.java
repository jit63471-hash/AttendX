package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentRepository {
    private static final Logger log = LoggerFactory.getLogger(StudentRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private static final String SELECT_BASE =
            "SELECT s.*, d.name as department_name, c.name as course_name, sem.label as semester_label " +
            "FROM students s " +
            "LEFT JOIN departments d ON s.department_id = d.id " +
            "LEFT JOIN courses c ON s.course_id = c.id " +
            "LEFT JOIN semesters sem ON s.semester_id = sem.id ";

    public List<Student> findAll() {
        List<Student> list = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY s.registration_number";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error retrieving all students", e);
            throw new DatabaseException("Failed to retrieve students", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Student> findActive() {
        List<Student> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE s.is_active = 1 ORDER BY s.registration_number";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error retrieving active students", e);
            throw new DatabaseException("Failed to retrieve active students", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Student> findById(int id) {
        String sql = SELECT_BASE + "WHERE s.id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding student by id: {}", id, e);
            throw new DatabaseException("Failed to find student", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<Student> findByRegistrationNumber(String regNo) {
        String sql = SELECT_BASE + "WHERE s.registration_number = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, regNo.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding student by regNo: {}", regNo, e);
            throw new DatabaseException("Failed to find student", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public List<Student> findByGroup(int departmentId, int courseId, int semesterId) {
        List<Student> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE s.department_id = ? AND s.course_id = ? AND s.semester_id = ? AND s.is_active = 1 " +
                "ORDER BY s.registration_number";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, departmentId);
            pstmt.setInt(2, courseId);
            pstmt.setInt(3, semesterId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding students by group", e);
            throw new DatabaseException("Failed to find students by group", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public List<Student> search(String query) {
        List<Student> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE s.registration_number LIKE ? OR s.first_name LIKE ? OR s.last_name LIKE ? " +
                "ORDER BY s.registration_number";
        String wildcard = "%" + query.trim() + "%";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, wildcard);
            pstmt.setString(2, wildcard);
            pstmt.setString(3, wildcard);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error searching students with query: {}", query, e);
            throw new DatabaseException("Failed to search students", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public boolean save(Student s) {
        String sql = "INSERT INTO students (registration_number, first_name, last_name, email, phone, " +
                "department_id, course_id, semester_id, section, academic_year, profile_picture_path, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, s.getRegistrationNumber().trim());
            pstmt.setString(2, s.getFirstName().trim());
            pstmt.setString(3, s.getLastName().trim());
            pstmt.setString(4, s.getEmail().trim());
            pstmt.setString(5, s.getPhone());
            pstmt.setInt(6, s.getDepartmentId());
            pstmt.setInt(7, s.getCourseId());
            pstmt.setInt(8, s.getSemesterId());
            pstmt.setString(9, s.getSection() != null ? s.getSection() : "A");
            pstmt.setString(10, s.getAcademicYear());
            pstmt.setString(11, s.getProfilePicturePath());
            pstmt.setInt(12, s.isActive() ? 1 : 0);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error saving student: {}", s.getRegistrationNumber(), e);
            throw new DatabaseException("Failed to save student: " + e.getMessage(), e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean update(Student s) {
        String sql = "UPDATE students SET registration_number = ?, first_name = ?, last_name = ?, email = ?, phone = ?, " +
                "department_id = ?, course_id = ?, semester_id = ?, section = ?, academic_year = ?, " +
                "profile_picture_path = ?, is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getRegistrationNumber().trim());
            pstmt.setString(2, s.getFirstName().trim());
            pstmt.setString(3, s.getLastName().trim());
            pstmt.setString(4, s.getEmail().trim());
            pstmt.setString(5, s.getPhone());
            pstmt.setInt(6, s.getDepartmentId());
            pstmt.setInt(7, s.getCourseId());
            pstmt.setInt(8, s.getSemesterId());
            pstmt.setString(9, s.getSection());
            pstmt.setString(10, s.getAcademicYear());
            pstmt.setString(11, s.getProfilePicturePath());
            pstmt.setInt(12, s.isActive() ? 1 : 0);
            pstmt.setInt(13, s.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating student: {}", s.getId(), e);
            throw new DatabaseException("Failed to update student", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean setActiveStatus(int id, boolean active) {
        String sql = "UPDATE students SET is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error changing student status: {}", id, e);
            throw new DatabaseException("Failed to update student status", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public int countActive() {
        String sql = "SELECT COUNT(*) FROM students WHERE is_active = 1";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error counting students", e);
        } finally {
            db.releaseConnection(conn);
        }
        return 0;
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setRegistrationNumber(rs.getString("registration_number"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setDepartmentId(rs.getInt("department_id"));
        s.setDepartmentName(rs.getString("department_name"));
        s.setCourseId(rs.getInt("course_id"));
        s.setCourseName(rs.getString("course_name"));
        s.setSemesterId(rs.getInt("semester_id"));
        s.setSemesterLabel(rs.getString("semester_label"));
        s.setSection(rs.getString("section"));
        s.setAcademicYear(rs.getString("academic_year"));
        s.setProfilePicturePath(rs.getString("profile_picture_path"));
        s.setActive(rs.getInt("is_active") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }
}
