package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.Semester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SemesterRepository {
    private static final Logger log = LoggerFactory.getLogger(SemesterRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<Semester> findAll() {
        List<Semester> list = new ArrayList<>();
        String sql = "SELECT * FROM semesters WHERE is_active = 1 ORDER BY number";
        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Semester(rs.getInt("id"), rs.getInt("number"), rs.getString("label"), rs.getInt("is_active") == 1));
            }
        } catch (SQLException e) {
            log.error("Error finding semesters", e);
            throw new DatabaseException("Failed to find semesters", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public Optional<Semester> findById(int id) {
        String sql = "SELECT * FROM semesters WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Semester(rs.getInt("id"), rs.getInt("number"), rs.getString("label"), rs.getInt("is_active") == 1));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding semester by id: {}", id, e);
            throw new DatabaseException("Failed to find semester", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }
}
