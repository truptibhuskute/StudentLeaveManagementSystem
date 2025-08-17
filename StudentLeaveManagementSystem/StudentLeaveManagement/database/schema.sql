-- Student Leave Management System Database Schema
-- MySQL Database Schema for comprehensive leave management

-- Create database
CREATE DATABASE IF NOT EXISTS student_leave_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_leave_db;

-- Students table
CREATE TABLE IF NOT EXISTS students (
    student_id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    department VARCHAR(100) NOT NULL,
    year VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL, -- Should be hashed in production
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_student_email (email),
    INDEX idx_student_department (department),
    INDEX idx_student_year (year),
    INDEX idx_student_active (is_active)
);

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    admin_id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    password VARCHAR(255) NOT NULL, -- Should be hashed in production
    role ENUM('SUPER_ADMIN', 'DEPARTMENT_HEAD', 'ACADEMIC_COORDINATOR', 'ASSISTANT_ADMIN', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    department VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_admin_email (email),
    INDEX idx_admin_role (role),
    INDEX idx_admin_department (department),
    INDEX idx_admin_active (is_active)
);

-- Leaves table
CREATE TABLE IF NOT EXISTS leaves (
    leave_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    leave_type ENUM('SICK', 'PERSONAL', 'EMERGENCY', 'MEDICAL', 'FAMILY', 'ACADEMIC', 'OTHER') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    admin_comments TEXT,
    approved_by VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES admins(admin_id) ON DELETE SET NULL,
    
    INDEX idx_leave_student (student_id),
    INDEX idx_leave_status (status),
    INDEX idx_leave_type (leave_type),
    INDEX idx_leave_dates (start_date, end_date),
    INDEX idx_leave_created (created_at),
    INDEX idx_leave_approved_by (approved_by),
    
    -- Business constraint: end_date should be >= start_date
    CHECK (end_date >= start_date),
    -- Business constraint: duration should not exceed 30 days
    CHECK (DATEDIFF(end_date, start_date) + 1 <= 30)
);

-- Leave attachments table (for future enhancement)
CREATE TABLE IF NOT EXISTS leave_attachments (
    attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (leave_id) REFERENCES leaves(leave_id) ON DELETE CASCADE,
    INDEX idx_attachment_leave (leave_id)
);

-- Leave history table (for audit trail)
CREATE TABLE IF NOT EXISTS leave_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_id BIGINT NOT NULL,
    action ENUM('CREATED', 'UPDATED', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL,
    old_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'),
    new_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'),
    performed_by VARCHAR(20),
    comments TEXT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (leave_id) REFERENCES leaves(leave_id) ON DELETE CASCADE,
    INDEX idx_history_leave (leave_id),
    INDEX idx_history_date (performed_at),
    INDEX idx_history_action (action)
);

-- Departments table (for reference data)
CREATE TABLE IF NOT EXISTS departments (
    department_id VARCHAR(10) PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    department_head VARCHAR(20),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (department_head) REFERENCES admins(admin_id) ON DELETE SET NULL,
    INDEX idx_dept_active (is_active)
);

-- Leave types configuration table (for dynamic leave types)
CREATE TABLE IF NOT EXISTS leave_types_config (
    type_id VARCHAR(20) PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL,
    description TEXT,
    max_days_per_request INT DEFAULT 30,
    max_days_per_year INT DEFAULT 365,
    requires_approval BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_leave_type_active (is_active)
);

-- System settings table
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_by VARCHAR(20),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (updated_by) REFERENCES admins(admin_id) ON DELETE SET NULL
);

-- Insert default departments
INSERT IGNORE INTO departments (department_id, department_name, description) VALUES
('CS', 'Computer Science', 'Department of Computer Science and Engineering'),
('EC', 'Electronics', 'Department of Electronics and Communication'),
('ME', 'Mechanical', 'Department of Mechanical Engineering'),
('CE', 'Civil', 'Department of Civil Engineering'),
('EE', 'Electrical', 'Department of Electrical Engineering'),
('IT', 'Information Technology', 'Department of Information Technology'),
('MBA', 'Business Administration', 'Master of Business Administration'),
('MCA', 'Computer Applications', 'Master of Computer Applications');

-- Insert default leave types configuration
INSERT IGNORE INTO leave_types_config (type_id, type_name, description, max_days_per_request, max_days_per_year) VALUES
('SICK', 'Sick Leave', 'Medical leave for illness', 15, 30),
('PERSONAL', 'Personal Leave', 'Personal reasons leave', 7, 15),
('EMERGENCY', 'Emergency Leave', 'Emergency situations', 10, 20),
('MEDICAL', 'Medical Leave', 'Medical treatment leave', 30, 90),
('FAMILY', 'Family Leave', 'Family related leave', 10, 30),
('ACADEMIC', 'Academic Leave', 'Academic activities leave', 15, 45),
('OTHER', 'Other', 'Other types of leave', 5, 10);

-- Insert default system settings
INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('MAX_LEAVE_DURATION', '30', 'Maximum leave duration in days'),
('AUTO_APPROVE_THRESHOLD', '1', 'Auto approve leaves less than this many days'),
('NOTIFICATION_EMAIL', 'admin@college.edu', 'System notification email'),
('ACADEMIC_YEAR_START', '2024-07-01', 'Academic year start date'),
('ACADEMIC_YEAR_END', '2025-06-30', 'Academic year end date'),
('LEAVE_APPLY_ADVANCE_DAYS', '1', 'Minimum advance days for leave application');

-- Insert default admin user (password: admin123)
INSERT IGNORE INTO admins (admin_id, first_name, last_name, email, password, role, department) VALUES
('ADMIN001', 'System', 'Administrator', 'admin@college.edu', 'admin123', 'SUPER_ADMIN', 'Administration');

-- Insert sample students for testing
INSERT IGNORE INTO students (student_id, first_name, last_name, email, department, year, password) VALUES
('CS2021001', 'John', 'Doe', 'john.doe@student.edu', 'Computer Science', '2021', 'student123'),
('CS2021002', 'Jane', 'Smith', 'jane.smith@student.edu', 'Computer Science', '2021', 'student123'),
('EC2020001', 'Mike', 'Johnson', 'mike.johnson@student.edu', 'Electronics', '2020', 'student123'),
('ME2021001', 'Sarah', 'Wilson', 'sarah.wilson@student.edu', 'Mechanical', '2021', 'student123'),
('IT2022001', 'David', 'Brown', 'david.brown@student.edu', 'Information Technology', '2022', 'student123');

-- Create views for common queries
CREATE OR REPLACE VIEW active_leaves AS
SELECT 
    l.*,
    s.first_name,
    s.last_name,
    s.department,
    DATEDIFF(l.end_date, l.start_date) + 1 as duration_days
FROM leaves l
JOIN students s ON l.student_id = s.student_id
WHERE l.status = 'APPROVED' 
AND CURDATE() BETWEEN l.start_date AND l.end_date;

CREATE OR REPLACE VIEW pending_leaves AS
SELECT 
    l.*,
    s.first_name,
    s.last_name,
    s.department,
    DATEDIFF(l.end_date, l.start_date) + 1 as duration_days
FROM leaves l
JOIN students s ON l.student_id = s.student_id
WHERE l.status = 'PENDING'
ORDER BY l.created_at ASC;

CREATE OR REPLACE VIEW leave_statistics AS
SELECT 
    s.department,
    COUNT(*) as total_leaves,
    SUM(CASE WHEN l.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_leaves,
    SUM(CASE WHEN l.status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_leaves,
    SUM(CASE WHEN l.status = 'PENDING' THEN 1 ELSE 0 END) as pending_leaves,
    AVG(DATEDIFF(l.end_date, l.start_date) + 1) as avg_duration_days
FROM leaves l
JOIN students s ON l.student_id = s.student_id
WHERE l.created_at >= DATE_SUB(CURDATE(), INTERVAL 1 YEAR)
GROUP BY s.department;
