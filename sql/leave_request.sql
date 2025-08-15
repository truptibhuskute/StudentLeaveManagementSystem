
CREATE DATABASE IF NOT EXISTS student_portal;
USE student_portal;

DROP TABLE IF EXISTS leave_requests;
CREATE TABLE leave_requests (
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_name VARCHAR(100) NOT NULL,
  roll_no VARCHAR(40) NOT NULL,
  from_date DATE NOT NULL,
  to_date DATE NOT NULL,
  reason TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'Pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
