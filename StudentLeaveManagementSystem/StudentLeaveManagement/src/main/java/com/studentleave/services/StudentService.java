package com.studentleave.services;

import com.studentleave.database.DatabaseConnection;
import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Student;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for Student operations
 * Demonstrates business logic implementation and database operations
 */
public class StudentService {
    private static final Logger logger = Logger.getLogger(StudentService.class.getName());

    /**
     * Register a new student
     */
    public boolean registerStudent(Student student) throws LeaveManagementException {
        if (student == null) {
            throw new LeaveManagementException("Student object cannot be null", 
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        validateStudentData(student);

        String sql = """
            INSERT INTO students (student_id, first_name, last_name, email, 
                                phone_number, department, year, password, is_active, 
                                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            // Check if student already exists
            if (studentExists(connection, student.getStudentId())) {
                throw new LeaveManagementException("Student ID already exists: " + student.getStudentId(),
                    "DUPLICATE_STUDENT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                LocalDateTime now = LocalDateTime.now();
                stmt.setString(1, student.getStudentId());
                stmt.setString(2, student.getFirstName());
                stmt.setString(3, student.getLastName());
                stmt.setString(4, student.getEmail());
                stmt.setString(5, student.getPhoneNumber());
                stmt.setString(6, student.getDepartment());
                stmt.setString(7, student.getYear());
                stmt.setString(8, student.getPassword()); // In production, hash this password
                stmt.setBoolean(9, student.isActive());
                stmt.setTimestamp(10, Timestamp.valueOf(now));
                stmt.setTimestamp(11, Timestamp.valueOf(now));

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Student registered successfully: " + student.getStudentId());
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during student registration", e);
            throw new LeaveManagementException("Failed to register student: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Authenticate student login
     */
    public Student authenticateStudent(String studentId, String password) throws LeaveManagementException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new LeaveManagementException("Student ID cannot be empty",
                "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
        }

        if (password == null || password.isEmpty()) {
            throw new LeaveManagementException("Password cannot be empty",
                "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
        }

        String sql = """
            SELECT student_id, first_name, last_name, email, phone_number, 
                   department, year, is_active, created_at, updated_at
            FROM students 
            WHERE student_id = ? AND password = ? AND is_active = TRUE
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, studentId.trim());
                stmt.setString(2, password); // In production, hash and compare

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Student student = new Student();
                        student.setStudentId(rs.getString("student_id"));
                        student.setFirstName(rs.getString("first_name"));
                        student.setLastName(rs.getString("last_name"));
                        student.setEmail(rs.getString("email"));
                        student.setPhoneNumber(rs.getString("phone_number"));
                        student.setDepartment(rs.getString("department"));
                        student.setYear(rs.getString("year"));
                        student.setActive(rs.getBoolean("is_active"));
                        student.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                        student.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                        logger.info("Student authenticated successfully: " + studentId);
                        return student;
                    } else {
                        logger.warning("Authentication failed for student: " + studentId);
                        throw new LeaveManagementException("Invalid credentials",
                            "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during student authentication", e);
            throw new LeaveManagementException("Authentication failed: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get student by ID
     */
    public Student getStudentById(String studentId) throws LeaveManagementException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new LeaveManagementException("Student ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT student_id, first_name, last_name, email, phone_number, 
                   department, year, is_active, created_at, updated_at
            FROM students 
            WHERE student_id = ?
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, studentId.trim());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Student student = new Student();
                        student.setStudentId(rs.getString("student_id"));
                        student.setFirstName(rs.getString("first_name"));
                        student.setLastName(rs.getString("last_name"));
                        student.setEmail(rs.getString("email"));
                        student.setPhoneNumber(rs.getString("phone_number"));
                        student.setDepartment(rs.getString("department"));
                        student.setYear(rs.getString("year"));
                        student.setActive(rs.getBoolean("is_active"));
                        student.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                        student.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                        return student;
                    } else {
                        throw new LeaveManagementException("Student not found: " + studentId,
                            "STUDENT_NOT_FOUND", LeaveManagementException.ErrorType.VALIDATION_ERROR);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching student", e);
            throw new LeaveManagementException("Failed to fetch student: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get all students (for admin use)
     */
    public List<Student> getAllStudents() throws LeaveManagementException {
        String sql = """
            SELECT student_id, first_name, last_name, email, phone_number, 
                   department, year, is_active, created_at, updated_at
            FROM students 
            ORDER BY department, year, last_name, first_name
        """;

        List<Student> students = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Student student = new Student();
                    student.setStudentId(rs.getString("student_id"));
                    student.setFirstName(rs.getString("first_name"));
                    student.setLastName(rs.getString("last_name"));
                    student.setEmail(rs.getString("email"));
                    student.setPhoneNumber(rs.getString("phone_number"));
                    student.setDepartment(rs.getString("department"));
                    student.setYear(rs.getString("year"));
                    student.setActive(rs.getBoolean("is_active"));
                    student.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    student.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                    students.add(student);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching all students", e);
            throw new LeaveManagementException("Failed to fetch students: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return students;
    }

    /**
     * Get students by department
     */
    public List<Student> getStudentsByDepartment(String department) throws LeaveManagementException {
        if (department == null || department.trim().isEmpty()) {
            throw new LeaveManagementException("Department cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT student_id, first_name, last_name, email, phone_number, 
                   department, year, is_active, created_at, updated_at
            FROM students 
            WHERE department = ? AND is_active = TRUE
            ORDER BY year, last_name, first_name
        """;

        List<Student> students = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, department.trim());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Student student = new Student();
                        student.setStudentId(rs.getString("student_id"));
                        student.setFirstName(rs.getString("first_name"));
                        student.setLastName(rs.getString("last_name"));
                        student.setEmail(rs.getString("email"));
                        student.setPhoneNumber(rs.getString("phone_number"));
                        student.setDepartment(rs.getString("department"));
                        student.setYear(rs.getString("year"));
                        student.setActive(rs.getBoolean("is_active"));
                        student.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                        student.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                        students.add(student);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching students by department", e);
            throw new LeaveManagementException("Failed to fetch students: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return students;
    }

    /**
     * Update student information
     */
    public boolean updateStudent(Student student) throws LeaveManagementException {
        if (student == null || student.getStudentId() == null) {
            throw new LeaveManagementException("Student and Student ID cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        validateStudentData(student);

        String sql = """
            UPDATE students 
            SET first_name = ?, last_name = ?, email = ?, phone_number = ?, 
                department = ?, year = ?, updated_at = ?
            WHERE student_id = ? AND is_active = TRUE
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, student.getFirstName());
                stmt.setString(2, student.getLastName());
                stmt.setString(3, student.getEmail());
                stmt.setString(4, student.getPhoneNumber());
                stmt.setString(5, student.getDepartment());
                stmt.setString(6, student.getYear());
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(8, student.getStudentId());

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Student updated successfully: " + student.getStudentId());
                } else {
                    logger.warning("No student found to update: " + student.getStudentId());
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during student update", e);
            throw new LeaveManagementException("Failed to update student: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Deactivate student (soft delete)
     */
    public boolean deactivateStudent(String studentId) throws LeaveManagementException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new LeaveManagementException("Student ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = "UPDATE students SET is_active = FALSE, updated_at = ? WHERE student_id = ?";

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, studentId.trim());

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Student deactivated successfully: " + studentId);
                } else {
                    logger.warning("No student found to deactivate: " + studentId);
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during student deactivation", e);
            throw new LeaveManagementException("Failed to deactivate student: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    // Private helper methods
    private boolean studentExists(Connection connection, String studentId) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE student_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void validateStudentData(Student student) throws LeaveManagementException {
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            throw new LeaveManagementException("Student ID is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (student.getFirstName() == null || student.getFirstName().trim().isEmpty()) {
            throw new LeaveManagementException("First name is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (student.getLastName() == null || student.getLastName().trim().isEmpty()) {
            throw new LeaveManagementException("Last name is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (student.getEmail() == null || !isValidEmail(student.getEmail())) {
            throw new LeaveManagementException("Valid email is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (student.getDepartment() == null || student.getDepartment().trim().isEmpty()) {
            throw new LeaveManagementException("Department is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (student.getYear() == null || student.getYear().trim().isEmpty()) {
            throw new LeaveManagementException("Year is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
