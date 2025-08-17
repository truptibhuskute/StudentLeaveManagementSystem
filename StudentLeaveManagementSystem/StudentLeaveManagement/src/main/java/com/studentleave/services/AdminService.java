package com.studentleave.services;

import com.studentleave.database.DatabaseConnection;
import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Admin;
import com.studentleave.models.Admin.AdminRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for Admin operations
 * Demonstrates admin management and role-based access control
 */
public class AdminService {
    private static final Logger logger = Logger.getLogger(AdminService.class.getName());

    /**
     * Authenticate admin login
     */
    public Admin authenticateAdmin(String adminId, String password) throws LeaveManagementException {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new LeaveManagementException("Admin ID cannot be empty",
                "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
        }

        if (password == null || password.isEmpty()) {
            throw new LeaveManagementException("Password cannot be empty",
                "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
        }

        String sql = """
            SELECT admin_id, first_name, last_name, email, phone_number, role,
                   department, is_active, created_at, updated_at, last_login_at
            FROM admins 
            WHERE admin_id = ? AND password = ? AND is_active = TRUE
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, adminId.trim());
                stmt.setString(2, password); // In production, hash and compare

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Admin admin = mapResultSetToAdmin(rs);
                        
                        // Update last login time
                        updateLastLogin(connection, adminId);
                        admin.recordLogin();
                        
                        logger.info("Admin authenticated successfully: " + adminId);
                        return admin;
                    } else {
                        logger.warning("Authentication failed for admin: " + adminId);
                        throw new LeaveManagementException("Invalid credentials",
                            "INVALID_CREDENTIALS", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during admin authentication", e);
            throw new LeaveManagementException("Authentication failed: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Register a new admin
     */
    public boolean registerAdmin(Admin admin) throws LeaveManagementException {
        if (admin == null) {
            throw new LeaveManagementException("Admin object cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        validateAdminData(admin);

        String sql = """
            INSERT INTO admins (admin_id, first_name, last_name, email, phone_number,
                              password, role, department, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            // Check if admin already exists
            if (adminExists(connection, admin.getAdminId())) {
                throw new LeaveManagementException("Admin ID already exists: " + admin.getAdminId(),
                    "DUPLICATE_ADMIN", LeaveManagementException.ErrorType.VALIDATION_ERROR);
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                LocalDateTime now = LocalDateTime.now();
                stmt.setString(1, admin.getAdminId());
                stmt.setString(2, admin.getFirstName());
                stmt.setString(3, admin.getLastName());
                stmt.setString(4, admin.getEmail());
                stmt.setString(5, admin.getPhoneNumber());
                stmt.setString(6, admin.getPassword()); // In production, hash this password
                stmt.setString(7, admin.getRole().name());
                stmt.setString(8, admin.getDepartment());
                stmt.setBoolean(9, admin.isActive());
                stmt.setTimestamp(10, Timestamp.valueOf(now));
                stmt.setTimestamp(11, Timestamp.valueOf(now));

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Admin registered successfully: " + admin.getAdminId());
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during admin registration", e);
            throw new LeaveManagementException("Failed to register admin: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get admin by ID
     */
    public Admin getAdminById(String adminId) throws LeaveManagementException {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new LeaveManagementException("Admin ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT admin_id, first_name, last_name, email, phone_number, role,
                   department, is_active, created_at, updated_at, last_login_at
            FROM admins 
            WHERE admin_id = ?
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, adminId.trim());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToAdmin(rs);
                    } else {
                        throw new LeaveManagementException("Admin not found: " + adminId,
                            "ADMIN_NOT_FOUND", LeaveManagementException.ErrorType.VALIDATION_ERROR);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching admin", e);
            throw new LeaveManagementException("Failed to fetch admin: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get all admins (for super admin use)
     */
    public List<Admin> getAllAdmins() throws LeaveManagementException {
        String sql = """
            SELECT admin_id, first_name, last_name, email, phone_number, role,
                   department, is_active, created_at, updated_at, last_login_at
            FROM admins 
            ORDER BY role, department, last_name, first_name
        """;

        List<Admin> admins = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    admins.add(mapResultSetToAdmin(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching all admins", e);
            throw new LeaveManagementException("Failed to fetch admins: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return admins;
    }

    /**
     * Get admins by role
     */
    public List<Admin> getAdminsByRole(AdminRole role) throws LeaveManagementException {
        if (role == null) {
            throw new LeaveManagementException("Role cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT admin_id, first_name, last_name, email, phone_number, role,
                   department, is_active, created_at, updated_at, last_login_at
            FROM admins 
            WHERE role = ? AND is_active = TRUE
            ORDER BY department, last_name, first_name
        """;

        List<Admin> admins = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, role.name());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        admins.add(mapResultSetToAdmin(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching admins by role", e);
            throw new LeaveManagementException("Failed to fetch admins: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return admins;
    }

    /**
     * Update admin information
     */
    public boolean updateAdmin(Admin admin) throws LeaveManagementException {
        if (admin == null || admin.getAdminId() == null) {
            throw new LeaveManagementException("Admin and Admin ID cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        validateAdminData(admin);

        String sql = """
            UPDATE admins 
            SET first_name = ?, last_name = ?, email = ?, phone_number = ?, 
                role = ?, department = ?, updated_at = ?
            WHERE admin_id = ? AND is_active = TRUE
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, admin.getFirstName());
                stmt.setString(2, admin.getLastName());
                stmt.setString(3, admin.getEmail());
                stmt.setString(4, admin.getPhoneNumber());
                stmt.setString(5, admin.getRole().name());
                stmt.setString(6, admin.getDepartment());
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(8, admin.getAdminId());

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Admin updated successfully: " + admin.getAdminId());
                } else {
                    logger.warning("No admin found to update: " + admin.getAdminId());
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during admin update", e);
            throw new LeaveManagementException("Failed to update admin: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Change admin password
     */
    public boolean changePassword(String adminId, String oldPassword, String newPassword) throws LeaveManagementException {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new LeaveManagementException("Admin ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new LeaveManagementException("New password must be at least 8 characters long",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            // First verify old password
            String verifySQL = "SELECT admin_id FROM admins WHERE admin_id = ? AND password = ? AND is_active = TRUE";
            try (PreparedStatement verifyStmt = connection.prepareStatement(verifySQL)) {
                verifyStmt.setString(1, adminId.trim());
                verifyStmt.setString(2, oldPassword);
                
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new LeaveManagementException("Invalid current password",
                            "INVALID_PASSWORD", LeaveManagementException.ErrorType.AUTHENTICATION_ERROR);
                    }
                }
            }

            // Update password
            String updateSQL = "UPDATE admins SET password = ?, updated_at = ? WHERE admin_id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                updateStmt.setString(1, newPassword); // In production, hash this
                updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setString(3, adminId.trim());

                int result = updateStmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Password changed successfully for admin: " + adminId);
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during password change", e);
            throw new LeaveManagementException("Failed to change password: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Deactivate admin (soft delete)
     */
    public boolean deactivateAdmin(String adminId) throws LeaveManagementException {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new LeaveManagementException("Admin ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = "UPDATE admins SET is_active = FALSE, updated_at = ? WHERE admin_id = ?";

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, adminId.trim());

                int result = stmt.executeUpdate();
                boolean success = result > 0;
                
                if (success) {
                    logger.info("Admin deactivated successfully: " + adminId);
                } else {
                    logger.warning("No admin found to deactivate: " + adminId);
                }
                
                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during admin deactivation", e);
            throw new LeaveManagementException("Failed to deactivate admin: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Check if admin has permission for a specific operation
     */
    public boolean hasPermission(String adminId, AdminRole requiredRole) throws LeaveManagementException {
        try {
            Admin admin = getAdminById(adminId);
            return admin.getRole().hasPermission(requiredRole);
        } catch (LeaveManagementException e) {
            if ("ADMIN_NOT_FOUND".equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
    }

    // Private helper methods
    private boolean adminExists(Connection connection, String adminId) throws SQLException {
        String sql = "SELECT 1 FROM admins WHERE admin_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void updateLastLogin(Connection connection, String adminId) throws SQLException {
        String sql = "UPDATE admins SET last_login_at = ? WHERE admin_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, adminId);
            stmt.executeUpdate();
        }
    }

    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        Admin admin = new Admin();
        admin.setAdminId(rs.getString("admin_id"));
        admin.setFirstName(rs.getString("first_name"));
        admin.setLastName(rs.getString("last_name"));
        admin.setEmail(rs.getString("email"));
        admin.setPhoneNumber(rs.getString("phone_number"));
        admin.setRole(AdminRole.valueOf(rs.getString("role")));
        admin.setDepartment(rs.getString("department"));
        admin.setActive(rs.getBoolean("is_active"));
        admin.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        admin.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        if (lastLoginAt != null) {
            admin.setLastLoginAt(lastLoginAt.toLocalDateTime());
        }
        
        return admin;
    }

    private void validateAdminData(Admin admin) throws LeaveManagementException {
        if (admin.getAdminId() == null || admin.getAdminId().trim().isEmpty()) {
            throw new LeaveManagementException("Admin ID is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (admin.getFirstName() == null || admin.getFirstName().trim().isEmpty()) {
            throw new LeaveManagementException("First name is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (admin.getLastName() == null || admin.getLastName().trim().isEmpty()) {
            throw new LeaveManagementException("Last name is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (admin.getEmail() == null || !isValidEmail(admin.getEmail())) {
            throw new LeaveManagementException("Valid email is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (admin.getRole() == null) {
            throw new LeaveManagementException("Role is required",
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
