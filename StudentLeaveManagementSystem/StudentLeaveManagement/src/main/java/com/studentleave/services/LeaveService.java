package com.studentleave.services;

import com.studentleave.database.DatabaseConnection;
import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Leave;
import com.studentleave.models.Leave.LeaveStatus;
import com.studentleave.models.Leave.LeaveType;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for Leave operations
 * Demonstrates complex business logic and database operations
 */
public class LeaveService {
    private static final Logger logger = Logger.getLogger(LeaveService.class.getName());

    /**
     * Apply for a new leave
     */
    public Long applyForLeave(Leave leave) throws LeaveManagementException {
        if (leave == null) {
            throw new LeaveManagementException("Leave object cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        validateLeaveApplication(leave);
        checkBusinessRules(leave);

        String sql = """
            INSERT INTO leaves (student_id, leave_type, start_date, end_date, reason, 
                              status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime now = LocalDateTime.now();
                stmt.setString(1, leave.getStudentId());
                stmt.setString(2, leave.getLeaveType().name());
                stmt.setDate(3, Date.valueOf(leave.getStartDate()));
                stmt.setDate(4, Date.valueOf(leave.getEndDate()));
                stmt.setString(5, leave.getReason());
                stmt.setString(6, LeaveStatus.PENDING.name());
                stmt.setTimestamp(7, Timestamp.valueOf(now));
                stmt.setTimestamp(8, Timestamp.valueOf(now));

                int result = stmt.executeUpdate();
                
                if (result > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Long leaveId = generatedKeys.getLong(1);
                            logger.info("Leave application submitted successfully: " + leaveId);
                            
                            // Record in history
                            recordLeaveHistory(connection, leaveId, "CREATED", null, 
                                LeaveStatus.PENDING, leave.getStudentId(), "Leave application submitted");
                            
                            return leaveId;
                        }
                    }
                }
                
                throw new LeaveManagementException("Failed to create leave application",
                    "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during leave application", e);
            throw new LeaveManagementException("Failed to apply for leave: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get leave by ID
     */
    public Leave getLeaveById(Long leaveId) throws LeaveManagementException {
        if (leaveId == null || leaveId <= 0) {
            throw new LeaveManagementException("Invalid leave ID",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT leave_id, student_id, leave_type, start_date, end_date, reason,
                   status, admin_comments, approved_by, created_at, updated_at, approved_at
            FROM leaves
            WHERE leave_id = ?
        """;

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, leaveId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToLeave(rs);
                    } else {
                        throw new LeaveManagementException("Leave not found: " + leaveId,
                            "LEAVE_NOT_FOUND", LeaveManagementException.ErrorType.VALIDATION_ERROR);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching leave", e);
            throw new LeaveManagementException("Failed to fetch leave: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get all leaves for a student
     */
    public List<Leave> getLeavesByStudentId(String studentId) throws LeaveManagementException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new LeaveManagementException("Student ID cannot be empty",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT leave_id, student_id, leave_type, start_date, end_date, reason,
                   status, admin_comments, approved_by, created_at, updated_at, approved_at
            FROM leaves
            WHERE student_id = ?
            ORDER BY created_at DESC
        """;

        List<Leave> leaves = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, studentId.trim());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        leaves.add(mapResultSetToLeave(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching student leaves", e);
            throw new LeaveManagementException("Failed to fetch leaves: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return leaves;
    }

    /**
     * Get pending leaves (for admin)
     */
    public List<Leave> getPendingLeaves() throws LeaveManagementException {
        String sql = """
            SELECT l.leave_id, l.student_id, l.leave_type, l.start_date, l.end_date, l.reason,
                   l.status, l.admin_comments, l.approved_by, l.created_at, l.updated_at, l.approved_at
            FROM leaves l
            WHERE l.status = 'PENDING'
            ORDER BY l.created_at ASC
        """;

        List<Leave> leaves = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    leaves.add(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching pending leaves", e);
            throw new LeaveManagementException("Failed to fetch pending leaves: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return leaves;
    }

    /**
     * Get leaves by status
     */
    public List<Leave> getLeavesByStatus(LeaveStatus status) throws LeaveManagementException {
        if (status == null) {
            throw new LeaveManagementException("Status cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT leave_id, student_id, leave_type, start_date, end_date, reason,
                   status, admin_comments, approved_by, created_at, updated_at, approved_at
            FROM leaves
            WHERE status = ?
            ORDER BY created_at DESC
        """;

        List<Leave> leaves = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        leaves.add(mapResultSetToLeave(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching leaves by status", e);
            throw new LeaveManagementException("Failed to fetch leaves: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return leaves;
    }

    /**
     * Approve a leave
     */
    public boolean approveLeave(Long leaveId, String adminId, String comments) throws LeaveManagementException {
        return updateLeaveStatus(leaveId, LeaveStatus.APPROVED, adminId, comments);
    }

    /**
     * Reject a leave
     */
    public boolean rejectLeave(Long leaveId, String adminId, String comments) throws LeaveManagementException {
        return updateLeaveStatus(leaveId, LeaveStatus.REJECTED, adminId, comments);
    }

    /**
     * Cancel a leave (by student)
     */
    public boolean cancelLeave(Long leaveId, String studentId) throws LeaveManagementException {
        if (leaveId == null || leaveId <= 0) {
            throw new LeaveManagementException("Invalid leave ID",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            // First, verify the leave belongs to the student and can be cancelled
            Leave leave = getLeaveById(leaveId);
            if (!leave.getStudentId().equals(studentId)) {
                throw new LeaveManagementException("Unauthorized to cancel this leave",
                    "UNAUTHORIZED", LeaveManagementException.ErrorType.AUTHORIZATION_ERROR);
            }

            if (!leave.canBeCancelled()) {
                throw new LeaveManagementException("Leave cannot be cancelled at this time",
                    "BUSINESS_RULE_VIOLATION", LeaveManagementException.ErrorType.BUSINESS_LOGIC_ERROR);
            }

            String sql = """
                UPDATE leaves 
                SET status = ?, updated_at = ?
                WHERE leave_id = ? AND student_id = ?
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, LeaveStatus.CANCELLED.name());
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setLong(3, leaveId);
                stmt.setString(4, studentId);

                int result = stmt.executeUpdate();
                boolean success = result > 0;

                if (success) {
                    logger.info("Leave cancelled successfully: " + leaveId + " by student: " + studentId);
                    recordLeaveHistory(connection, leaveId, "CANCELLED", leave.getStatus(), 
                        LeaveStatus.CANCELLED, studentId, "Leave cancelled by student");
                }

                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during leave cancellation", e);
            throw new LeaveManagementException("Failed to cancel leave: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    /**
     * Get active leaves (currently ongoing)
     */
    public List<Leave> getActiveLeaves() throws LeaveManagementException {
        String sql = """
            SELECT leave_id, student_id, leave_type, start_date, end_date, reason,
                   status, admin_comments, approved_by, created_at, updated_at, approved_at
            FROM leaves
            WHERE status = 'APPROVED' 
            AND CURDATE() BETWEEN start_date AND end_date
            ORDER BY start_date ASC
        """;

        List<Leave> leaves = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    leaves.add(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching active leaves", e);
            throw new LeaveManagementException("Failed to fetch active leaves: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return leaves;
    }

    /**
     * Get leaves within date range
     */
    public List<Leave> getLeavesBetweenDates(LocalDate startDate, LocalDate endDate) throws LeaveManagementException {
        if (startDate == null || endDate == null) {
            throw new LeaveManagementException("Start date and end date cannot be null",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (endDate.isBefore(startDate)) {
            throw new LeaveManagementException("End date cannot be before start date",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        String sql = """
            SELECT leave_id, student_id, leave_type, start_date, end_date, reason,
                   status, admin_comments, approved_by, created_at, updated_at, approved_at
            FROM leaves
            WHERE (start_date BETWEEN ? AND ?) OR (end_date BETWEEN ? AND ?)
               OR (start_date <= ? AND end_date >= ?)
            ORDER BY start_date ASC
        """;

        List<Leave> leaves = new ArrayList<>();
        Connection connection = null;
        
        try {
            connection = DatabaseConnection.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                Date sqlStartDate = Date.valueOf(startDate);
                Date sqlEndDate = Date.valueOf(endDate);
                
                stmt.setDate(1, sqlStartDate);
                stmt.setDate(2, sqlEndDate);
                stmt.setDate(3, sqlStartDate);
                stmt.setDate(4, sqlEndDate);
                stmt.setDate(5, sqlStartDate);
                stmt.setDate(6, sqlEndDate);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        leaves.add(mapResultSetToLeave(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching leaves by date range", e);
            throw new LeaveManagementException("Failed to fetch leaves: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }

        return leaves;
    }

    // Private helper methods
    private boolean updateLeaveStatus(Long leaveId, LeaveStatus newStatus, String adminId, String comments) 
            throws LeaveManagementException {
        if (leaveId == null || leaveId <= 0) {
            throw new LeaveManagementException("Invalid leave ID",
                "INVALID_INPUT", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            
            // Get current leave details
            Leave leave = getLeaveById(leaveId);
            
            if (leave.getStatus() != LeaveStatus.PENDING) {
                throw new LeaveManagementException("Only pending leaves can be approved or rejected",
                    "INVALID_STATUS", LeaveManagementException.ErrorType.BUSINESS_LOGIC_ERROR);
            }

            String sql = """
                UPDATE leaves 
                SET status = ?, admin_comments = ?, approved_by = ?, approved_at = ?, updated_at = ?
                WHERE leave_id = ?
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                LocalDateTime now = LocalDateTime.now();
                stmt.setString(1, newStatus.name());
                stmt.setString(2, comments);
                stmt.setString(3, adminId);
                stmt.setTimestamp(4, Timestamp.valueOf(now));
                stmt.setTimestamp(5, Timestamp.valueOf(now));
                stmt.setLong(6, leaveId);

                int result = stmt.executeUpdate();
                boolean success = result > 0;

                if (success) {
                    logger.info("Leave " + newStatus.name().toLowerCase() + " successfully: " + leaveId + 
                              " by admin: " + adminId);
                    
                    String action = newStatus == LeaveStatus.APPROVED ? "APPROVED" : "REJECTED";
                    recordLeaveHistory(connection, leaveId, action, LeaveStatus.PENDING, 
                        newStatus, adminId, comments);
                }

                return success;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during leave status update", e);
            throw new LeaveManagementException("Failed to update leave status: " + e.getMessage(), e,
                "DB_ERROR", LeaveManagementException.ErrorType.DATABASE_ERROR);
        } finally {
            DatabaseConnection.returnConnection(connection);
        }
    }

    private void validateLeaveApplication(Leave leave) throws LeaveManagementException {
        if (leave.getStudentId() == null || leave.getStudentId().trim().isEmpty()) {
            throw new LeaveManagementException("Student ID is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (leave.getLeaveType() == null) {
            throw new LeaveManagementException("Leave type is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (leave.getStartDate() == null) {
            throw new LeaveManagementException("Start date is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (leave.getEndDate() == null) {
            throw new LeaveManagementException("End date is required",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }

        if (leave.getReason() == null || leave.getReason().trim().length() < 10) {
            throw new LeaveManagementException("Reason must be at least 10 characters long",
                "VALIDATION_ERROR", LeaveManagementException.ErrorType.VALIDATION_ERROR);
        }
    }

    private void checkBusinessRules(Leave leave) throws LeaveManagementException {
        // Check if dates are valid
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new LeaveManagementException("End date cannot be before start date",
                "BUSINESS_RULE_VIOLATION", LeaveManagementException.ErrorType.BUSINESS_LOGIC_ERROR);
        }

        // Check if applying for future dates
        if (leave.getStartDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new LeaveManagementException("Leave must be applied at least 1 day in advance",
                "BUSINESS_RULE_VIOLATION", LeaveManagementException.ErrorType.BUSINESS_LOGIC_ERROR);
        }

        // Check maximum duration
        long duration = leave.getDurationInDays();
        if (duration > 30) {
            throw new LeaveManagementException("Leave duration cannot exceed 30 days",
                "BUSINESS_RULE_VIOLATION", LeaveManagementException.ErrorType.BUSINESS_LOGIC_ERROR);
        }

        // Check for overlapping leaves (simplified check)
        // In a real application, this would be more comprehensive
        // For now, we'll skip this check to keep the example simpler
    }

    private Leave mapResultSetToLeave(ResultSet rs) throws SQLException {
        Leave leave = new Leave();
        leave.setLeaveId(rs.getLong("leave_id"));
        leave.setStudentId(rs.getString("student_id"));
        leave.setLeaveType(LeaveType.valueOf(rs.getString("leave_type")));
        leave.setStartDate(rs.getDate("start_date").toLocalDate());
        leave.setEndDate(rs.getDate("end_date").toLocalDate());
        leave.setReason(rs.getString("reason"));
        leave.setStatus(LeaveStatus.valueOf(rs.getString("status")));
        leave.setAdminComments(rs.getString("admin_comments"));
        leave.setApprovedBy(rs.getString("approved_by"));
        leave.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        leave.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) {
            leave.setApprovedAt(approvedAt.toLocalDateTime());
        }
        
        return leave;
    }

    private void recordLeaveHistory(Connection connection, Long leaveId, String action, 
                                   LeaveStatus oldStatus, LeaveStatus newStatus, 
                                   String performedBy, String comments) throws SQLException {
        String sql = """
            INSERT INTO leave_history (leave_id, action, old_status, new_status, 
                                     performed_by, comments, performed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, leaveId);
            stmt.setString(2, action);
            stmt.setString(3, oldStatus != null ? oldStatus.name() : null);
            stmt.setString(4, newStatus.name());
            stmt.setString(5, performedBy);
            stmt.setString(6, comments);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            
            stmt.executeUpdate();
        }
    }
}
