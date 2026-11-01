package com.attendx.config;

import com.attendx.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;

public class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;

    private String url;
    private String username;
    private String password;
    private int maxPoolSize = 10;
    private boolean isMySql = true;

    private final LinkedList<Connection> connectionPool = new LinkedList<>();

    private DatabaseConfig() {
        loadProperties();
        initDatabase();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config/database.properties")) {
            if (in != null) {
                props.load(in);
                this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/attendx_db");
                this.username = props.getProperty("db.username", "root");
                this.password = props.getProperty("db.password", "root");
                this.maxPoolSize = Integer.parseInt(props.getProperty("db.pool.size", "10"));
            } else {
                log.warn("database.properties not found on classpath, using defaults");
                this.url = "jdbc:mysql://localhost:3306/attendx_db";
                this.username = "root";
                this.password = "root";
            }
        } catch (Exception e) {
            log.error("Failed to load database.properties", e);
        }
    }

    private void initDatabase() {
        // Try MySQL first
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection testConn = DriverManager.getConnection(this.url, this.username, this.password);
            testConn.close();
            this.isMySql = true;
            log.info("Successfully connected to MySQL database: {}", this.url);
        } catch (Exception ex) {
            log.warn("Could not connect to MySQL ({}). Falling back to SQLite for standalone mode.", ex.getMessage());
            this.isMySql = false;
            this.url = "jdbc:sqlite:attendx.db";
            this.username = "";
            this.password = "";
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                log.error("SQLite JDBC Driver not found", e);
            }
        }

        // Initialize schema
        initializeSchema();
    }

    private void initializeSchema() {
        try (Connection conn = createNewConnection(); Statement stmt = conn.createStatement()) {
            if (isMySql) {
                // Tables created via sql/01_schema.sql or inline verification
                stmt.execute("CREATE TABLE IF NOT EXISTS roles (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE)");
                stmt.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(200) NOT NULL, email VARCHAR(200) UNIQUE, role_id INT NOT NULL, is_active BOOLEAN DEFAULT TRUE, last_login DATETIME NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS departments (id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL, description TEXT, is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS courses (id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL, department_id INT NOT NULL, duration_semesters INT NOT NULL DEFAULT 8, is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS semesters (id INT AUTO_INCREMENT PRIMARY KEY, number INT NOT NULL, label VARCHAR(50) NOT NULL, is_active BOOLEAN DEFAULT TRUE)");
                stmt.execute("CREATE TABLE IF NOT EXISTS subjects (id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL, course_id INT NOT NULL, semester_id INT NOT NULL, credits INT DEFAULT 3, is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS students (id INT AUTO_INCREMENT PRIMARY KEY, registration_number VARCHAR(50) NOT NULL UNIQUE, user_id INT NULL UNIQUE, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, email VARCHAR(200) NOT NULL UNIQUE, phone VARCHAR(20), department_id INT NOT NULL, course_id INT NOT NULL, semester_id INT NOT NULL, section VARCHAR(10) DEFAULT 'A', academic_year VARCHAR(20) NOT NULL, profile_picture_path VARCHAR(500), is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS faculty (id INT AUTO_INCREMENT PRIMARY KEY, employee_id VARCHAR(50) NOT NULL UNIQUE, user_id INT NOT NULL UNIQUE, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, email VARCHAR(200) NOT NULL UNIQUE, phone VARCHAR(20), department_id INT NOT NULL, designation VARCHAR(100) DEFAULT 'Assistant Professor', is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                stmt.execute("CREATE TABLE IF NOT EXISTS class_sessions (id INT AUTO_INCREMENT PRIMARY KEY, subject_id INT NOT NULL, faculty_id INT NOT NULL, session_date DATE NOT NULL, start_time TIME NULL, end_time TIME NULL, topic VARCHAR(500), status VARCHAR(20) DEFAULT 'COMPLETED', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_subject_date (subject_id, session_date))");
                stmt.execute("CREATE TABLE IF NOT EXISTS attendance (id INT AUTO_INCREMENT PRIMARY KEY, class_session_id INT NOT NULL, student_id INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ABSENT', remarks VARCHAR(500), marked_by INT NULL, marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY uq_session_student (class_session_id, student_id))");
                stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NULL, action VARCHAR(100) NOT NULL, entity_type VARCHAR(100), entity_id INT, old_values TEXT, new_values TEXT, ip_address VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            } else {
                // SQLite equivalent schema
                stmt.execute("CREATE TABLE IF NOT EXISTS roles (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);");
                stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, full_name TEXT NOT NULL, email TEXT UNIQUE, role_id INTEGER NOT NULL, is_active INTEGER DEFAULT 1, last_login TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);");
                stmt.execute("CREATE TABLE IF NOT EXISTS departments (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT, is_active INTEGER DEFAULT 1);");
                stmt.execute("CREATE TABLE IF NOT EXISTS courses (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, department_id INTEGER NOT NULL, duration_semesters INTEGER DEFAULT 8, is_active INTEGER DEFAULT 1);");
                stmt.execute("CREATE TABLE IF NOT EXISTS semesters (id INTEGER PRIMARY KEY AUTOINCREMENT, number INTEGER NOT NULL, label TEXT NOT NULL, is_active INTEGER DEFAULT 1);");
                stmt.execute("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, course_id INTEGER NOT NULL, semester_id INTEGER NOT NULL, credits INTEGER DEFAULT 3, is_active INTEGER DEFAULT 1);");
                stmt.execute("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, registration_number TEXT NOT NULL UNIQUE, user_id INTEGER, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, phone TEXT, department_id INTEGER NOT NULL, course_id INTEGER NOT NULL, semester_id INTEGER NOT NULL, section TEXT DEFAULT 'A', academic_year TEXT NOT NULL, profile_picture_path TEXT, is_active INTEGER DEFAULT 1, created_at TEXT DEFAULT CURRENT_TIMESTAMP);");
                stmt.execute("CREATE TABLE IF NOT EXISTS faculty (id INTEGER PRIMARY KEY AUTOINCREMENT, employee_id TEXT NOT NULL UNIQUE, user_id INTEGER NOT NULL UNIQUE, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, phone TEXT, department_id INTEGER NOT NULL, designation TEXT, is_active INTEGER DEFAULT 1, created_at TEXT DEFAULT CURRENT_TIMESTAMP);");
                stmt.execute("CREATE TABLE IF NOT EXISTS class_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, subject_id INTEGER NOT NULL, faculty_id INTEGER NOT NULL, session_date TEXT NOT NULL, start_time TEXT, end_time TEXT, topic TEXT, status TEXT DEFAULT 'COMPLETED', UNIQUE(subject_id, session_date));");
                stmt.execute("CREATE TABLE IF NOT EXISTS attendance (id INTEGER PRIMARY KEY AUTOINCREMENT, class_session_id INTEGER NOT NULL, student_id INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'ABSENT', remarks TEXT, marked_by INTEGER, marked_at TEXT DEFAULT CURRENT_TIMESTAMP, UNIQUE(class_session_id, student_id));");
                stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, action TEXT NOT NULL, entity_type TEXT, entity_id INTEGER, old_values TEXT, new_values TEXT, ip_address TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);");
            }

            // Seed master data if empty
            seedInitialData(conn);
        } catch (SQLException e) {
            log.error("Failed to initialize database tables", e);
        }
    }

    private void seedInitialData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            if (isMySql) {
                stmt.execute("INSERT IGNORE INTO roles (id, name) VALUES (1, 'admin'), (2, 'faculty')");
                // Default admin: admin / Admin@123
                stmt.execute("INSERT IGNORE INTO users (id, username, password_hash, full_name, email, role_id, is_active) " +
                        "VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'System Administrator', 'admin@attendx.edu', 1, TRUE)");
                stmt.execute("INSERT IGNORE INTO departments (id, code, name, description, is_active) VALUES " +
                        "(1, 'CSE', 'Computer Science & Engineering', 'Dept of CSE', TRUE), " +
                        "(2, 'IT', 'Information Technology', 'Dept of IT', TRUE), " +
                        "(3, 'ECE', 'Electronics & Communication', 'Dept of ECE', TRUE)");
                stmt.execute("INSERT IGNORE INTO courses (id, code, name, department_id, duration_semesters, is_active) VALUES " +
                        "(1, 'BTECH-CSE', 'B.Tech Computer Science', 1, 8, TRUE), " +
                        "(2, 'BTECH-IT', 'B.Tech Information Technology', 2, 8, TRUE)");
                stmt.execute("INSERT IGNORE INTO semesters (id, number, label, is_active) VALUES " +
                        "(1, 1, 'Semester 1', TRUE), (2, 2, 'Semester 2', TRUE), (3, 3, 'Semester 3', TRUE), (4, 4, 'Semester 4', TRUE)");
                stmt.execute("INSERT IGNORE INTO subjects (id, code, name, course_id, semester_id, credits, is_active) VALUES " +
                        "(1, 'CS101', 'Data Structures & Algorithms', 1, 1, 4, TRUE), " +
                        "(2, 'CS102', 'Java Programming', 1, 1, 4, TRUE), " +
                        "(3, 'CS103', 'Database Management', 1, 1, 3, TRUE)");
                stmt.execute("INSERT IGNORE INTO users (id, username, password_hash, full_name, email, role_id, is_active) " +
                        "VALUES (2, 'faculty1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'Dr. Alan Turing', 'alan@attendx.edu', 2, TRUE)");
                stmt.execute("INSERT IGNORE INTO faculty (id, employee_id, user_id, first_name, last_name, email, phone, department_id, designation, is_active) " +
                        "VALUES (1, 'FAC001', 2, 'Alan', 'Turing', 'alan@attendx.edu', '9876543210', 1, 'Professor & HOD', TRUE)");
                stmt.execute("INSERT IGNORE INTO students (id, registration_number, first_name, last_name, email, phone, department_id, course_id, semester_id, section, academic_year, is_active) VALUES " +
                        "(1, 'CS2025001', 'John', 'Doe', 'john@attendx.edu', '9876500001', 1, 1, 1, 'A', '2025-2026', TRUE), " +
                        "(2, 'CS2025002', 'Jane', 'Smith', 'jane@attendx.edu', '9876500002', 1, 1, 1, 'A', '2025-2026', TRUE), " +
                        "(3, 'CS2025003', 'Robert', 'Johnson', 'robert@attendx.edu', '9876500003', 1, 1, 1, 'A', '2025-2026', TRUE)");
            } else {
                stmt.execute("INSERT OR IGNORE INTO roles (id, name) VALUES (1, 'admin'), (2, 'faculty');");
                stmt.execute("INSERT OR IGNORE INTO users (id, username, password_hash, full_name, email, role_id, is_active) " +
                        "VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'System Administrator', 'admin@attendx.edu', 1, 1);");
                stmt.execute("INSERT OR IGNORE INTO departments (id, code, name, description, is_active) VALUES " +
                        "(1, 'CSE', 'Computer Science & Engineering', 'Dept of CSE', 1), " +
                        "(2, 'IT', 'Information Technology', 'Dept of IT', 1), " +
                        "(3, 'ECE', 'Electronics & Communication', 'Dept of ECE', 1);");
                stmt.execute("INSERT OR IGNORE INTO courses (id, code, name, department_id, duration_semesters, is_active) VALUES " +
                        "(1, 'BTECH-CSE', 'B.Tech Computer Science', 1, 8, 1), " +
                        "(2, 'BTECH-IT', 'B.Tech Information Technology', 2, 8, 1);");
                stmt.execute("INSERT OR IGNORE INTO semesters (id, number, label, is_active) VALUES " +
                        "(1, 1, 'Semester 1', 1), (2, 2, 'Semester 2', 1), (3, 3, 'Semester 3', 1), (4, 4, 'Semester 4', 1);");
                stmt.execute("INSERT OR IGNORE INTO subjects (id, code, name, course_id, semester_id, credits, is_active) VALUES " +
                        "(1, 'CS101', 'Data Structures & Algorithms', 1, 1, 4, 1), " +
                        "(2, 'CS102', 'Java Programming', 1, 1, 4, 1), " +
                        "(3, 'CS103', 'Database Management', 1, 1, 3, 1);");
                stmt.execute("INSERT OR IGNORE INTO users (id, username, password_hash, full_name, email, role_id, is_active) " +
                        "VALUES (2, 'faculty1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'Dr. Alan Turing', 'alan@attendx.edu', 2, 1);");
                stmt.execute("INSERT OR IGNORE INTO faculty (id, employee_id, user_id, first_name, last_name, email, phone, department_id, designation, is_active) " +
                        "VALUES (1, 'FAC001', 2, 'Alan', 'Turing', 'alan@attendx.edu', '9876543210', 1, 'Professor & HOD', 1);");
                stmt.execute("INSERT OR IGNORE INTO students (id, registration_number, first_name, last_name, email, phone, department_id, course_id, semester_id, section, academic_year, is_active) VALUES " +
                        "(1, 'CS2025001', 'John', 'Doe', 'john@attendx.edu', '9876500001', 1, 1, 1, 'A', '2025-2026', 1), " +
                        "(2, 'CS2025002', 'Jane', 'Smith', 'jane@attendx.edu', '9876500002', 1, 1, 1, 'A', '2025-2026', 1), " +
                        "(3, 'CS2025003', 'Robert', 'Johnson', 'robert@attendx.edu', '9876500003', 1, 1, 1, 'A', '2025-2026', 1);");
            }
        } catch (SQLException e) {
            log.error("Failed to seed master data", e);
        }
    }

    private Connection createNewConnection() throws SQLException {
        if (isMySql) {
            return DriverManager.getConnection(this.url, this.username, this.password);
        } else {
            return DriverManager.getConnection(this.url);
        }
    }

    public synchronized Connection getConnection() {
        try {
            while (!connectionPool.isEmpty()) {
                Connection conn = connectionPool.removeFirst();
                if (conn != null && !conn.isClosed() && conn.isValid(2)) {
                    return conn;
                }
            }
            return createNewConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to acquire database connection: " + e.getMessage(), e);
        }
    }

    public synchronized void releaseConnection(Connection connection) {
        if (connection == null) return;
        try {
            if (!connection.isClosed() && connectionPool.size() < maxPoolSize) {
                connectionPool.addLast(connection);
            } else {
                connection.close();
            }
        } catch (SQLException e) {
            log.error("Error releasing connection to pool", e);
        }
    }

    public boolean isMySQL() {
        return isMySql;
    }

    public synchronized void shutdown() {
        for (Connection conn : connectionPool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error("Error closing pooled connection during shutdown", e);
            }
        }
        connectionPool.clear();
    }
}
