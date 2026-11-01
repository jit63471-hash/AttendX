package com.attendx.repository;

import com.attendx.config.DatabaseConfig;
import com.attendx.exception.DatabaseException;
import com.attendx.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.role_id, r.name as role_name, " +
                "u.is_active, u.last_login, u.created_at " +
                "FROM users u JOIN roles r ON u.role_id = r.id " +
                "WHERE u.username = ? AND u.is_active = 1";

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding user by username: {}", username, e);
            throw new DatabaseException("Database error finding user", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.role_id, r.name as role_name, " +
                "u.is_active, u.last_login, u.created_at " +
                "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?";

        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding user by id: {}", id, e);
            throw new DatabaseException("Database error finding user by ID", e);
        } finally {
            db.releaseConnection(conn);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.role_id, r.name as role_name, " +
                "u.is_active, u.last_login, u.created_at " +
                "FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.full_name";

        Connection conn = db.getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            log.error("Error listing users", e);
            throw new DatabaseException("Database error listing users", e);
        } finally {
            db.releaseConnection(conn);
        }
        return list;
    }

    public int save(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role_id, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getEmail());
            pstmt.setInt(5, user.getRoleId());
            pstmt.setInt(6, user.isActive() ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                    return user.getId();
                }
            }
            return 1;
        } catch (SQLException e) {
            log.error("Error saving user: {}", user.getUsername(), e);
            throw new DatabaseException("Failed to create user: " + e.getMessage(), e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, is_active = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setInt(3, user.isActive() ? 1 : 0);
            pstmt.setInt(4, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating user: {}", user.getId(), e);
            throw new DatabaseException("Failed to update user", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating password for user: {}", userId, e);
            throw new DatabaseException("Failed to update password", e);
        } finally {
            db.releaseConnection(conn);
        }
    }

    public boolean updateLastLogin(int userId) {
        String sql = db.isMySQL() ? "UPDATE users SET last_login = NOW() WHERE id = ?"
                : "UPDATE users SET last_login = datetime('now') WHERE id = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("Could not update last login for user: {}", userId, e);
            return false;
        } finally {
            db.releaseConnection(conn);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setActive(rs.getInt("is_active") == 1);
        Timestamp lastLoginTs = rs.getTimestamp("last_login");
        if (lastLoginTs != null) {
            u.setLastLogin(lastLoginTs.toLocalDateTime());
        }
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            u.setCreatedAt(createdTs.toLocalDateTime());
        }
        return u;
    }
}
