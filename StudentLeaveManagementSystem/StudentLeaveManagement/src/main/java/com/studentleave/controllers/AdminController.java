package com.studentleave.controllers;

import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Admin;
import com.studentleave.models.Admin.AdminRole;
import com.studentleave.models.Leave;
import com.studentleave.models.Student;
import com.studentleave.services.AdminService;
import com.studentleave.services.LeaveService;
import com.studentleave.services.ReportingService;
import com.studentleave.services.StudentService;
import com.studentleave.utils.InputUtils;

import java.util.List;

/**
 * Controller class for handling admin-related operations
 * Demonstrates admin dashboard and role-based access control
 */
public class AdminController {
    private final AdminService adminService;
    private final StudentService studentService;
    private final LeaveService leaveService;
    private final ReportingService reportingService;

    public AdminController() {
        this.adminService = new AdminService();
        this.studentService = new StudentService();
        this.leaveService = new LeaveService();
        this.reportingService = new ReportingService();
    }

    /**
     * Handle admin registration
     */
    public void handleAdminRegistration() {
        try {
            InputUtils.displayHeader("ADMIN REGISTRATION");
            InputUtils.displayWarning("Admin registration requires Super Admin privileges.");
            
            // For demo purposes, we'll allow registration without authentication
            // In production, this should require Super Admin authentication
            
            // Collect admin information
            String adminId = InputUtils.readString("Admin ID");
            String firstName = InputUtils.readString("First Name");
            String lastName = InputUtils.readString("Last Name");
            String email = InputUtils.readEmail("Email");
            String phoneNumber = InputUtils.readOptionalString("Phone Number");
            String password = InputUtils.readPassword("Password", 8);
            String department = InputUtils.readOptionalString("Department");
            
            // Show role options
            System.out.println("\nAvailable Admin Roles:");
            AdminRole[] roles = AdminRole.values();
            for (int i = 0; i < roles.length; i++) {
                System.out.printf("%d. %s (Level %d)%n", i + 1, 
                    roles[i].getDisplayName(), roles[i].getPermissionLevel());
            }
            
            int roleChoice = InputUtils.readInt("Select role", 1, roles.length);
            AdminRole role = roles[roleChoice - 1];
            
            // Create admin object
            Admin admin = new Admin(adminId, firstName, lastName, email, password, role, department);
            
            // Register admin
            boolean success = adminService.registerAdmin(admin);
            
            if (success) {
                InputUtils.displaySuccess("Admin registered successfully!");
                InputUtils.displayInfo("Admin can now login using Admin ID and password.");
            } else {
                InputUtils.displayError("Registration failed. Please try again.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Handle admin login
     */
    public void handleAdminLogin() {
        try {
            InputUtils.displayHeader("ADMIN LOGIN");
            
            String adminId = InputUtils.readString("Admin ID");
            String password = InputUtils.readPassword("Password", 1);
            
            // Authenticate admin
            Admin admin = adminService.authenticateAdmin(adminId, password);
            
            InputUtils.displaySuccess("Login successful! Welcome, " + admin.getFullName());
            InputUtils.displayInfo("Role: " + admin.getRole().getDisplayName());
            
            // Start admin session
            runAdminSession(admin);
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Login failed: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Run admin session after successful login
     */
    private void runAdminSession(Admin admin) {
        while (true) {
            try {
                int choice = showAdminMenu(admin);
                
                switch (choice) {
                    case 1 -> viewDashboard(admin);
                    case 2 -> managePendingLeaves(admin);
                    case 3 -> viewAllLeaves(admin);
                    case 4 -> manageStudents(admin);
                    case 5 -> generateReports(admin);
                    case 6 -> manageAdmins(admin);
                    case 7 -> viewMyProfile(admin);
                    case 8 -> changePassword(admin);
                    case 0 -> {
                        InputUtils.displayInfo("Logged out successfully. Thank you!");
                        return;
                    }
                    default -> InputUtils.displayWarning("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                InputUtils.displayError("An error occurred: " + e.getMessage());
                InputUtils.waitForEnter();
            }
        }
    }

    /**
     * Show admin menu based on role permissions
     */
    private int showAdminMenu(Admin admin) {
        String[] options = {
            "Dashboard Overview",
            "Manage Pending Leaves",
            "View All Leaves",
            "Manage Students",
            "Generate Reports",
            admin.canManageUsers() ? "Manage Admins" : null,
            "View My Profile",
            "Change Password"
        };
        
        // Filter out null options (permissions-based)
        String[] filteredOptions = java.util.Arrays.stream(options)
            .filter(java.util.Objects::nonNull)
            .toArray(String[]::new);
        
        return InputUtils.readMenuChoice("ADMIN DASHBOARD - " + admin.getFullName() + 
            " (" + admin.getRole().getDisplayName() + ")", filteredOptions);
    }

    /**
     * View admin dashboard with key metrics
     */
    private void viewDashboard(Admin admin) {
        try {
            InputUtils.displayHeader("ADMIN DASHBOARD");
            
            // Get key metrics
            List<Leave> pendingLeaves = leaveService.getPendingLeaves();
            List<Leave> activeLeaves = leaveService.getActiveLeaves();
            List<Student> allStudents = studentService.getAllStudents();
            
            // Display overview
            System.out.println("System Overview:");
            System.out.println("  Total Students: " + allStudents.size());
            System.out.println("  Pending Leave Requests: " + pendingLeaves.size());
            System.out.println("  Active Leaves Today: " + activeLeaves.size());
            System.out.println();
            
            // Show recent pending leaves
            if (!pendingLeaves.isEmpty()) {
                System.out.println("Recent Pending Leave Requests:");
                int displayCount = Math.min(5, pendingLeaves.size());
                String[] headers = {"ID", "Student", "Type", "Start Date", "Duration", "Applied On"};
                String[][] data = new String[displayCount][6];
                
                for (int i = 0; i < displayCount; i++) {
                    Leave leave = pendingLeaves.get(i);
                    try {
                        Student student = studentService.getStudentById(leave.getStudentId());
                        data[i][0] = String.valueOf(leave.getLeaveId());
                        data[i][1] = student.getFullName();
                        data[i][2] = leave.getLeaveType().getDisplayName();
                        data[i][3] = leave.getStartDate().toString();
                        data[i][4] = leave.getDurationInDays() + " days";
                        data[i][5] = leave.getCreatedAt().toLocalDate().toString();
                    } catch (LeaveManagementException e) {
                        data[i][1] = "Unknown Student";
                    }
                }
                
                InputUtils.displayTable(headers, data);
            }
            
            // Show active leaves
            if (!activeLeaves.isEmpty()) {
                System.out.println("Currently Active Leaves:");
                int displayCount = Math.min(5, activeLeaves.size());
                String[] headers = {"Student", "Type", "Start Date", "End Date"};
                String[][] data = new String[displayCount][4];
                
                for (int i = 0; i < displayCount; i++) {
                    Leave leave = activeLeaves.get(i);
                    try {
                        Student student = studentService.getStudentById(leave.getStudentId());
                        data[i][0] = student.getFullName();
                        data[i][1] = leave.getLeaveType().getDisplayName();
                        data[i][2] = leave.getStartDate().toString();
                        data[i][3] = leave.getEndDate().toString();
                    } catch (LeaveManagementException e) {
                        data[i][0] = "Unknown Student";
                    }
                }
                
                InputUtils.displayTable(headers, data);
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to load dashboard: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Manage pending leaves
     */
    private void managePendingLeaves(Admin admin) {
        if (!admin.canApproveLeaves()) {
            InputUtils.displayError("You don't have permission to approve leaves.");
            InputUtils.waitForEnter();
            return;
        }
        
        try {
            InputUtils.displayHeader("MANAGE PENDING LEAVES");
            
            List<Leave> pendingLeaves = leaveService.getPendingLeaves();
            
            if (pendingLeaves.isEmpty()) {
                InputUtils.displayInfo("No pending leave requests found.");
                InputUtils.waitForEnter();
                return;
            }
            
            // Display pending leaves with student information
            String[] headers = {"ID", "Student", "Type", "Start Date", "End Date", "Duration", "Applied On"};
            String[][] data = new String[pendingLeaves.size()][7];
            
            for (int i = 0; i < pendingLeaves.size(); i++) {
                Leave leave = pendingLeaves.get(i);
                try {
                    Student student = studentService.getStudentById(leave.getStudentId());
                    data[i][0] = String.valueOf(leave.getLeaveId());
                    data[i][1] = student.getFullName() + " (" + student.getStudentId() + ")";
                    data[i][2] = leave.getLeaveType().getDisplayName();
                    data[i][3] = leave.getStartDate().toString();
                    data[i][4] = leave.getEndDate().toString();
                    data[i][5] = leave.getDurationInDays() + " days";
                    data[i][6] = leave.getCreatedAt().toLocalDate().toString();
                } catch (LeaveManagementException e) {
                    data[i][1] = "Unknown Student (" + leave.getStudentId() + ")";
                }
            }
            
            InputUtils.displayTable(headers, data);
            
            // Process leave
            if (InputUtils.readBoolean("Do you want to process a leave request")) {
                Long leaveId = InputUtils.readLong("Enter Leave ID to process");
                
                // Verify leave exists and is pending
                Leave leave = leaveService.getLeaveById(leaveId);
                if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
                    InputUtils.displayError("Leave is not in pending status.");
                    InputUtils.waitForEnter();
                    return;
                }
                
                // Show leave details
                displayLeaveDetails(leave);
                
                // Get action
                String[] actions = {"Approve", "Reject"};
                int action = InputUtils.readMenuChoice("Select Action", actions);
                
                if (action == 1) {
                    // Approve
                    String comments = InputUtils.readOptionalString("Comments (optional)");
                    boolean success = leaveService.approveLeave(leaveId, admin.getAdminId(), comments);
                    
                    if (success) {
                        InputUtils.displaySuccess("Leave approved successfully!");
                    } else {
                        InputUtils.displayError("Failed to approve leave.");
                    }
                } else if (action == 2) {
                    // Reject
                    String comments = InputUtils.readString("Reason for rejection", 5);
                    boolean success = leaveService.rejectLeave(leaveId, admin.getAdminId(), comments);
                    
                    if (success) {
                        InputUtils.displaySuccess("Leave rejected successfully!");
                    } else {
                        InputUtils.displayError("Failed to reject leave.");
                    }
                }
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to manage leaves: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * View all leaves with filtering options
     */
    private void viewAllLeaves(Admin admin) {
        try {
            InputUtils.displayHeader("ALL LEAVES");
            
            String[] filterOptions = {
                "All Leaves",
                "By Status",
                "Active Leaves",
                "By Student"
            };
            
            int filter = InputUtils.readMenuChoice("Filter Options", filterOptions);
            List<Leave> leaves = null;
            
            switch (filter) {
                case 1 -> {
                    // All leaves - this would need pagination in real application
                    InputUtils.displayInfo("Showing recent leaves (limited to avoid performance issues)");
                    leaves = leaveService.getPendingLeaves(); // For demo, show pending only
                    leaves.addAll(leaveService.getActiveLeaves());
                }
                case 2 -> {
                    // By status
                    Leave.LeaveStatus[] statuses = Leave.LeaveStatus.values();
                    System.out.println("Available Statuses:");
                    for (int i = 0; i < statuses.length; i++) {
                        System.out.printf("%d. %s%n", i + 1, statuses[i].getDisplayName());
                    }
                    int statusChoice = InputUtils.readInt("Select status", 1, statuses.length);
                    leaves = leaveService.getLeavesByStatus(statuses[statusChoice - 1]);
                }
                case 3 -> {
                    // Active leaves
                    leaves = leaveService.getActiveLeaves();
                }
                case 4 -> {
                    // By student
                    String studentId = InputUtils.readString("Enter Student ID");
                    leaves = leaveService.getLeavesByStudentId(studentId);
                }
            }
            
            if (leaves != null && !leaves.isEmpty()) {
                displayLeavesTable(leaves);
            } else {
                InputUtils.displayInfo("No leaves found matching the criteria.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to fetch leaves: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Manage students (view, search, update)
     */
    private void manageStudents(Admin admin) {
        try {
            InputUtils.displayHeader("MANAGE STUDENTS");
            
            String[] options = {
                "View All Students",
                "Search Student",
                "View Student Details",
                admin.canManageUsers() ? "Deactivate Student" : null
            };
            
            String[] filteredOptions = java.util.Arrays.stream(options)
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);
            
            int choice = InputUtils.readMenuChoice("Student Management", filteredOptions);
            
            switch (choice) {
                case 1 -> viewAllStudents();
                case 2 -> searchStudent();
                case 3 -> viewStudentDetails();
                case 4 -> {
                    if (admin.canManageUsers()) {
                        deactivateStudent();
                    }
                }
            }
            
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
            InputUtils.waitForEnter();
        }
    }

    /**
     * Generate various reports
     */
    private void generateReports(Admin admin) {
        if (!admin.canViewReports()) {
            InputUtils.displayError("You don't have permission to view reports.");
            InputUtils.waitForEnter();
            return;
        }
        
        try {
            InputUtils.displayHeader("GENERATE REPORTS");
            
            String[] reportTypes = {
                "Leave Statistics by Department",
                "Monthly Leave Summary",
                "Student Leave History",
                "Pending Approvals Report"
            };
            
            int reportType = InputUtils.readMenuChoice("Select Report Type", reportTypes);
            
            switch (reportType) {
                case 1 -> reportingService.generateDepartmentStatistics();
                case 2 -> reportingService.generateMonthlyReport();
                case 3 -> {
                    String studentId = InputUtils.readString("Enter Student ID");
                    reportingService.generateStudentReport(studentId);
                }
                case 4 -> reportingService.generatePendingApprovalsReport();
            }
            
        } catch (Exception e) {
            InputUtils.displayError("Failed to generate report: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    // Helper methods for admin operations
    private void displayLeaveDetails(Leave leave) {
        try {
            Student student = studentService.getStudentById(leave.getStudentId());
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("LEAVE REQUEST DETAILS");
            System.out.println("=".repeat(50));
            System.out.println("Leave ID: " + leave.getLeaveId());
            System.out.println("Student: " + student.getFullName() + " (" + student.getStudentId() + ")");
            System.out.println("Department: " + student.getDepartment());
            System.out.println("Year: " + student.getYear());
            System.out.println("Leave Type: " + leave.getLeaveType().getDisplayName());
            System.out.println("Duration: " + leave.getStartDate() + " to " + leave.getEndDate() + 
                             " (" + leave.getDurationInDays() + " days)");
            System.out.println("Reason: " + leave.getReason());
            System.out.println("Applied On: " + leave.getCreatedAt().toLocalDate());
            System.out.println("Status: " + leave.getStatus().getDisplayName());
            System.out.println("=".repeat(50));
            
        } catch (LeaveManagementException e) {
            System.out.println("Error loading student details: " + e.getMessage());
        }
    }

    private void displayLeavesTable(List<Leave> leaves) {
        String[] headers = {"ID", "Student", "Type", "Start Date", "End Date", "Duration", "Status", "Applied On"};
        String[][] data = new String[leaves.size()][8];
        
        for (int i = 0; i < leaves.size(); i++) {
            Leave leave = leaves.get(i);
            try {
                Student student = studentService.getStudentById(leave.getStudentId());
                data[i][0] = String.valueOf(leave.getLeaveId());
                data[i][1] = student.getFullName();
                data[i][2] = leave.getLeaveType().getDisplayName();
                data[i][3] = leave.getStartDate().toString();
                data[i][4] = leave.getEndDate().toString();
                data[i][5] = leave.getDurationInDays() + " days";
                data[i][6] = leave.getStatus().getDisplayName();
                data[i][7] = leave.getCreatedAt().toLocalDate().toString();
            } catch (LeaveManagementException e) {
                data[i][1] = "Unknown Student";
            }
        }
        
        InputUtils.displayTable(headers, data);
    }

    private void viewAllStudents() {
        try {
            List<Student> students = studentService.getAllStudents();
            
            if (students.isEmpty()) {
                InputUtils.displayInfo("No students found.");
                return;
            }
            
            String[] headers = {"Student ID", "Name", "Email", "Department", "Year", "Status"};
            String[][] data = new String[students.size()][6];
            
            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                data[i][0] = student.getStudentId();
                data[i][1] = student.getFullName();
                data[i][2] = student.getEmail();
                data[i][3] = student.getDepartment();
                data[i][4] = student.getYear();
                data[i][5] = student.isActive() ? "Active" : "Inactive";
            }
            
            InputUtils.displayTable(headers, data);
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to fetch students: " + e.getMessage());
        }
    }

    private void searchStudent() {
        try {
            String studentId = InputUtils.readString("Enter Student ID");
            Student student = studentService.getStudentById(studentId);
            
            System.out.println("\nStudent Information:");
            System.out.println("  Student ID: " + student.getStudentId());
            System.out.println("  Name: " + student.getFullName());
            System.out.println("  Email: " + student.getEmail());
            System.out.println("  Phone: " + (student.getPhoneNumber() != null ? student.getPhoneNumber() : "Not provided"));
            System.out.println("  Department: " + student.getDepartment());
            System.out.println("  Year: " + student.getYear());
            System.out.println("  Status: " + (student.isActive() ? "Active" : "Inactive"));
            System.out.println("  Member Since: " + student.getCreatedAt().toLocalDate());
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Student not found: " + e.getMessage());
        }
    }

    private void viewStudentDetails() {
        searchStudent();
    }

    private void deactivateStudent() {
        try {
            String studentId = InputUtils.readString("Enter Student ID to deactivate");
            
            // First show student details
            Student student = studentService.getStudentById(studentId);
            System.out.println("\nStudent to deactivate:");
            System.out.println("  " + student.getFullName() + " (" + student.getStudentId() + ")");
            
            boolean confirm = InputUtils.readBoolean("Are you sure you want to deactivate this student");
            
            if (confirm) {
                boolean success = studentService.deactivateStudent(studentId);
                if (success) {
                    InputUtils.displaySuccess("Student deactivated successfully!");
                } else {
                    InputUtils.displayError("Failed to deactivate student.");
                }
            } else {
                InputUtils.displayInfo("Deactivation cancelled.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to deactivate student: " + e.getMessage());
        }
    }

    private void manageAdmins(Admin admin) {
        if (!admin.canManageUsers()) {
            InputUtils.displayError("You don't have permission to manage admins.");
            InputUtils.waitForEnter();
            return;
        }
        
        try {
            InputUtils.displayHeader("MANAGE ADMINS");
            
            String[] options = {
                "View All Admins",
                "View Admin Details",
                admin.canManageSystem() ? "Deactivate Admin" : null
            };
            
            String[] filteredOptions = java.util.Arrays.stream(options)
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);
            
            int choice = InputUtils.readMenuChoice("Admin Management", filteredOptions);
            
            switch (choice) {
                case 1 -> viewAllAdmins();
                case 2 -> viewAdminDetails();
                case 3 -> {
                    if (admin.canManageSystem()) {
                        deactivateAdmin();
                    }
                }
            }
            
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    private void viewAllAdmins() {
        try {
            List<Admin> admins = adminService.getAllAdmins();
            
            if (admins.isEmpty()) {
                InputUtils.displayInfo("No admins found.");
                return;
            }
            
            String[] headers = {"Admin ID", "Name", "Email", "Role", "Department", "Status"};
            String[][] data = new String[admins.size()][6];
            
            for (int i = 0; i < admins.size(); i++) {
                Admin adminItem = admins.get(i);
                data[i][0] = adminItem.getAdminId();
                data[i][1] = adminItem.getFullName();
                data[i][2] = adminItem.getEmail();
                data[i][3] = adminItem.getRole().getDisplayName();
                data[i][4] = adminItem.getDepartment() != null ? adminItem.getDepartment() : "All";
                data[i][5] = adminItem.isActive() ? "Active" : "Inactive";
            }
            
            InputUtils.displayTable(headers, data);
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to fetch admins: " + e.getMessage());
        }
    }

    private void viewAdminDetails() {
        try {
            String adminId = InputUtils.readString("Enter Admin ID");
            Admin adminItem = adminService.getAdminById(adminId);
            
            System.out.println("\nAdmin Information:");
            System.out.println("  Admin ID: " + adminItem.getAdminId());
            System.out.println("  Name: " + adminItem.getFullName());
            System.out.println("  Email: " + adminItem.getEmail());
            System.out.println("  Phone: " + (adminItem.getPhoneNumber() != null ? adminItem.getPhoneNumber() : "Not provided"));
            System.out.println("  Role: " + adminItem.getRole().getDisplayName());
            System.out.println("  Department: " + (adminItem.getDepartment() != null ? adminItem.getDepartment() : "All"));
            System.out.println("  Status: " + (adminItem.isActive() ? "Active" : "Inactive"));
            System.out.println("  Created: " + adminItem.getCreatedAt().toLocalDate());
            System.out.println("  Last Login: " + (adminItem.getLastLoginAt() != null ? 
                adminItem.getLastLoginAt().toLocalDate() : "Never"));
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Admin not found: " + e.getMessage());
        }
    }

    private void deactivateAdmin() {
        try {
            String adminId = InputUtils.readString("Enter Admin ID to deactivate");
            
            // First show admin details
            Admin adminItem = adminService.getAdminById(adminId);
            System.out.println("\nAdmin to deactivate:");
            System.out.println("  " + adminItem.getFullName() + " (" + adminItem.getAdminId() + ")");
            System.out.println("  Role: " + adminItem.getRole().getDisplayName());
            
            boolean confirm = InputUtils.readBoolean("Are you sure you want to deactivate this admin");
            
            if (confirm) {
                boolean success = adminService.deactivateAdmin(adminId);
                if (success) {
                    InputUtils.displaySuccess("Admin deactivated successfully!");
                } else {
                    InputUtils.displayError("Failed to deactivate admin.");
                }
            } else {
                InputUtils.displayInfo("Deactivation cancelled.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to deactivate admin: " + e.getMessage());
        }
    }

    private void viewMyProfile(Admin admin) {
        InputUtils.displayHeader("MY PROFILE");
        
        System.out.println("Admin Information:");
        System.out.println("  Admin ID: " + admin.getAdminId());
        System.out.println("  Name: " + admin.getFullName());
        System.out.println("  Email: " + admin.getEmail());
        System.out.println("  Phone: " + (admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "Not provided"));
        System.out.println("  Role: " + admin.getRole().getDisplayName());
        System.out.println("  Department: " + (admin.getDepartment() != null ? admin.getDepartment() : "All"));
        System.out.println("  Status: " + (admin.isActive() ? "Active" : "Inactive"));
        System.out.println("  Created: " + admin.getCreatedAt().toLocalDate());
        System.out.println("  Last Login: " + (admin.getLastLoginAt() != null ? 
            admin.getLastLoginAt().toLocalDate() : "Never"));
        
        System.out.println("\nPermissions:");
        System.out.println("  Can Approve Leaves: " + (admin.canApproveLeaves() ? "Yes" : "No"));
        System.out.println("  Can Manage Users: " + (admin.canManageUsers() ? "Yes" : "No"));
        System.out.println("  Can View Reports: " + (admin.canViewReports() ? "Yes" : "No"));
        System.out.println("  Can Manage System: " + (admin.canManageSystem() ? "Yes" : "No"));
        
        InputUtils.waitForEnter();
    }

    private void changePassword(Admin admin) {
        try {
            InputUtils.displayHeader("CHANGE PASSWORD");
            
            String oldPassword = InputUtils.readPassword("Current Password", 1);
            String newPassword = InputUtils.readPassword("New Password", 8);
            String confirmPassword = InputUtils.readPassword("Confirm New Password", 8);
            
            if (!newPassword.equals(confirmPassword)) {
                InputUtils.displayError("Passwords do not match!");
                InputUtils.waitForEnter();
                return;
            }
            
            boolean success = adminService.changePassword(admin.getAdminId(), oldPassword, newPassword);
            
            if (success) {
                InputUtils.displaySuccess("Password changed successfully!");
            } else {
                InputUtils.displayError("Failed to change password. Please check your current password.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to change password: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }
}
