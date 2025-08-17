package com.studentleave.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Leave model class representing a leave request in the system
 * Demonstrates enum usage, business logic, and data validation
 */
public class Leave {
    private Long leaveId;
    private String studentId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private String adminComments;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;

    // Enums for type safety and better design
    public enum LeaveType {
        SICK("Sick Leave"),
        PERSONAL("Personal Leave"),
        EMERGENCY("Emergency Leave"),
        MEDICAL("Medical Leave"),
        FAMILY("Family Leave"),
        ACADEMIC("Academic Leave"),
        OTHER("Other");

        private final String displayName;

        LeaveType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum LeaveStatus {
        PENDING("Pending Approval"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        CANCELLED("Cancelled");

        private final String displayName;

        LeaveStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Default constructor
    public Leave() {
        this.status = LeaveStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Parameterized constructor
    public Leave(String studentId, LeaveType leaveType, LocalDate startDate, 
                 LocalDate endDate, String reason) {
        this();
        this.studentId = studentId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        validateDates();
    }

    // Getters and Setters with validation
    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        this.studentId = studentId.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        if (leaveType == null) {
            throw new IllegalArgumentException("Leave type cannot be null");
        }
        this.leaveType = leaveType;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        this.startDate = startDate;
        this.updatedAt = LocalDateTime.now();
        validateDates();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
        validateDates();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        if (reason.trim().length() < 10) {
            throw new IllegalArgumentException("Reason must be at least 10 characters long");
        }
        this.reason = reason.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAdminComments() {
        return adminComments;
    }

    public void setAdminComments(String adminComments) {
        this.adminComments = adminComments != null ? adminComments.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    // Business logic methods
    public long getDurationInDays() {
        if (startDate != null && endDate != null) {
            return ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return 0;
    }

    public boolean isActive() {
        return status == LeaveStatus.APPROVED && 
               LocalDate.now().isAfter(startDate.minusDays(1)) &&
               LocalDate.now().isBefore(endDate.plusDays(1));
    }

    public boolean isPending() {
        return status == LeaveStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == LeaveStatus.PENDING || 
               (status == LeaveStatus.APPROVED && LocalDate.now().isBefore(startDate));
    }

    public void approve(String adminId, String comments) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leaves can be approved");
        }
        this.status = LeaveStatus.APPROVED;
        this.approvedBy = adminId;
        this.adminComments = comments;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String adminId, String comments) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leaves can be rejected");
        }
        this.status = LeaveStatus.REJECTED;
        this.approvedBy = adminId;
        this.adminComments = comments;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Leave cannot be cancelled at this time");
        }
        this.status = LeaveStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateDates() {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("End date cannot be before start date");
            }
            if (startDate.isBefore(LocalDate.now()) && status == LeaveStatus.PENDING) {
                throw new IllegalArgumentException("Cannot apply for leave in the past");
            }
            if (getDurationInDays() > 30) {
                throw new IllegalArgumentException("Leave duration cannot exceed 30 days");
            }
        }
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Leave leave = (Leave) obj;
        return Objects.equals(leaveId, leave.leaveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leaveId);
    }

    @Override
    public String toString() {
        return String.format("Leave{id=%d, studentId='%s', type='%s', dates='%s to %s', status='%s', duration=%d days}",
                leaveId, studentId, leaveType.getDisplayName(), startDate, endDate, 
                status.getDisplayName(), getDurationInDays());
    }
}
