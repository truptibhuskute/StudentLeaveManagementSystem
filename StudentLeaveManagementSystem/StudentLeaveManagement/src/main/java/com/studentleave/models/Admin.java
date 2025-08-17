package com.studentleave.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Admin model class representing an administrator in the leave management system
 * Demonstrates role-based design and administrative functionality
 */
public class Admin {
    private String adminId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private AdminRole role;
    private String department;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Enum for admin roles with different permission levels
    public enum AdminRole {
        SUPER_ADMIN("Super Admin", 5),
        DEPARTMENT_HEAD("Department Head", 4),
        ACADEMIC_COORDINATOR("Academic Coordinator", 3),
        ASSISTANT_ADMIN("Assistant Admin", 2),
        VIEWER("Viewer", 1);

        private final String displayName;
        private final int permissionLevel;

        AdminRole(String displayName, int permissionLevel) {
            this.displayName = displayName;
            this.permissionLevel = permissionLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getPermissionLevel() {
            return permissionLevel;
        }

        public boolean hasPermission(AdminRole requiredRole) {
            return this.permissionLevel >= requiredRole.permissionLevel;
        }
    }

    // Default constructor
    public Admin() {
        this.role = AdminRole.VIEWER;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Parameterized constructor
    public Admin(String adminId, String firstName, String lastName, 
                 String email, String password, AdminRole role, String department) {
        this();
        this.adminId = adminId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
    }

    // Getters and Setters with validation
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin ID cannot be null or empty");
        }
        this.adminId = adminId.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        this.firstName = firstName.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        this.lastName = lastName.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email.toLowerCase().trim();
        this.updatedAt = LocalDateTime.now();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = phoneNumber.trim();
            this.updatedAt = LocalDateTime.now();
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        this.password = password;
        this.updatedAt = LocalDateTime.now();
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = role;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // Business logic methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean canApproveLeaves() {
        return role.hasPermission(AdminRole.ACADEMIC_COORDINATOR);
    }

    public boolean canManageUsers() {
        return role.hasPermission(AdminRole.DEPARTMENT_HEAD);
    }

    public boolean canViewReports() {
        return role.hasPermission(AdminRole.ASSISTANT_ADMIN);
    }

    public boolean canManageSystem() {
        return role.hasPermission(AdminRole.SUPER_ADMIN);
    }

    public boolean canManageDepartment(String targetDepartment) {
        if (role == AdminRole.SUPER_ADMIN) {
            return true;
        }
        if (role == AdminRole.DEPARTMENT_HEAD) {
            return department != null && department.equalsIgnoreCase(targetDepartment);
        }
        return false;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Admin admin = (Admin) obj;
        return Objects.equals(adminId, admin.adminId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adminId);
    }

    @Override
    public String toString() {
        return String.format("Admin{adminId='%s', name='%s', email='%s', role='%s', department='%s', active=%s}",
                adminId, getFullName(), email, role.getDisplayName(), department, isActive);
    }
}
