-- ==========================================================
-- AttendX – Seed Data
-- Default Administrative and Academic Master Data
-- ==========================================================

USE attendx_db;

-- 1. Roles
INSERT INTO roles (id, name) VALUES 
(1, 'admin'),
(2, 'faculty')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Administrator Users
-- Admin 1: Sudip (Password: Sudip123)
INSERT INTO users (username, password_hash, full_name, email, role_id, is_active) VALUES 
('Sudip', '$2a$10$oTGxPjPYZuwpKaVT4B.2teSDJ8qzAIJHJvJs6nLv/Ke.Pmq3BE4tS', 'Sudip (Administrator)', 'sudip@attendx.edu', 1, TRUE)
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash);

-- Admin 2: admin (Password: Admin@123)
INSERT INTO users (username, password_hash, full_name, email, role_id, is_active) VALUES 
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'System Administrator', 'admin@attendx.edu', 1, TRUE)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- Faculty user (Password: Admin@123)
INSERT INTO users (username, password_hash, full_name, email, role_id, is_active) VALUES 
('faculty1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'Dr. Alan Turing', 'alan.turing@attendx.edu', 2, TRUE)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

-- 3. Departments
INSERT INTO departments (id, code, name, description, is_active) VALUES
(1, 'CSE', 'Computer Science & Engineering', 'Department of Computer Science and Engineering', TRUE),
(2, 'IT', 'Information Technology', 'Department of Information Technology', TRUE),
(3, 'ECE', 'Electronics & Communication', 'Department of Electronics and Communication Engineering', TRUE),
(4, 'MECH', 'Mechanical Engineering', 'Department of Mechanical Engineering', TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 4. Courses
INSERT INTO courses (id, code, name, department_id, duration_semesters, is_active) VALUES
(1, 'BTECH-CSE', 'B.Tech Computer Science', 1, 8, TRUE),
(2, 'BTECH-IT', 'B.Tech Information Technology', 2, 8, TRUE),
(3, 'BTECH-ECE', 'B.Tech Electronics & Comm', 3, 8, TRUE),
(4, 'BTECH-MECH', 'B.Tech Mechanical', 4, 8, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 5. Semesters
INSERT INTO semesters (id, number, label, is_active) VALUES
(1, 1, 'Semester 1', TRUE),
(2, 2, 'Semester 2', TRUE),
(3, 3, 'Semester 3', TRUE),
(4, 4, 'Semester 4', TRUE),
(5, 5, 'Semester 5', TRUE),
(6, 6, 'Semester 6', TRUE),
(7, 7, 'Semester 7', TRUE),
(8, 8, 'Semester 8', TRUE)
ON DUPLICATE KEY UPDATE label=VALUES(label);

-- 6. Subjects (Sample for B.Tech CSE Semester 1 and 2)
INSERT INTO subjects (id, code, name, course_id, semester_id, credits, is_active) VALUES
(1, 'CS101', 'Data Structures & Algorithms', 1, 1, 4, TRUE),
(2, 'CS102', 'Object Oriented Programming with Java', 1, 1, 4, TRUE),
(3, 'CS103', 'Database Management Systems', 1, 1, 3, TRUE),
(4, 'CS104', 'Computer Architecture', 1, 1, 3, TRUE),
(5, 'CS201', 'Operating Systems', 1, 2, 4, TRUE),
(6, 'CS202', 'Computer Networks', 1, 2, 3, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 7. Faculty Profile
INSERT INTO faculty (id, employee_id, user_id, first_name, last_name, email, phone, department_id, designation, is_active) VALUES
(1, 'FAC001', 3, 'Alan', 'Turing', 'alan.turing@attendx.edu', '9876543210', 1, 'Professor & HOD', TRUE)
ON DUPLICATE KEY UPDATE designation=VALUES(designation);

-- 8. Faculty Assignment
INSERT INTO faculty_assignments (faculty_id, subject_id, academic_year) VALUES
(1, 1, '2025-2026'),
(1, 2, '2025-2026')
ON DUPLICATE KEY UPDATE academic_year=VALUES(academic_year);

-- 9. Sample Students
INSERT INTO students (id, registration_number, first_name, last_name, email, phone, department_id, course_id, semester_id, section, academic_year, is_active) VALUES
(1, 'CS2025001', 'John', 'Doe', 'john.doe@attendx.edu', '9876500001', 1, 1, 1, 'A', '2025-2026', TRUE),
(2, 'CS2025002', 'Jane', 'Smith', 'jane.smith@attendx.edu', '9876500002', 1, 1, 1, 'A', '2025-2026', TRUE),
(3, 'CS2025003', 'Robert', 'Johnson', 'robert.j@attendx.edu', '9876500003', 1, 1, 1, 'A', '2025-2026', TRUE),
(4, 'CS2025004', 'Emily', 'Davis', 'emily.d@attendx.edu', '9876500004', 1, 1, 1, 'A', '2025-2026', TRUE),
(5, 'CS2025005', 'Michael', 'Brown', 'michael.b@attendx.edu', '9876500005', 1, 1, 1, 'A', '2025-2026', TRUE)
ON DUPLICATE KEY UPDATE first_name=VALUES(first_name);
