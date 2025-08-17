package com.studentleave.controllers;

import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Leave;
import com.studentleave.models.Leave.LeaveType;
import com.studentleave.models.Student;
import com.studentleave.services.LeaveService;
import com.studentleave.services.StudentService;
import com.studentleave.utils.InputUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller class for handling student-related operations
 * Demonstrates MVC pattern and user interface logic
 */
public class StudentController {
    private final StudentService studentService;
    private final LeaveService leaveService;

    public StudentController() {
        this.studentService = new StudentService();
        this.leaveService = new LeaveService();
    }

    /**
     * Handle student registration
     */
    public void handleStudentRegistration() {
        try {
            InputUtils.displayHeader("STUDENT REGISTRATION");
            
            // Collect student information
            String studentId = InputUtils.readString("Student ID");
            String firstName = InputUtils.readString("First Name");
            String lastName = InputUtils.readString("Last Name");
            String email = InputUtils.readEmail("Email");
            String phoneNumber = InputUtils.readOptionalString("Phone Number");
            String department = InputUtils.readString("Department");
            String year = InputUtils.readString("Year");
            String password = InputUtils.readPassword("Password", 6);
            
            // Create student object
            Student student = new Student(studentId, firstName, lastName, email, 
                                        phoneNumber, department, year, password);
            
            // Register student
            boolean success = studentService.registerStudent(student);
            
            if (success) {
                InputUtils.displaySuccess("Student registered successfully!");
                InputUtils.displayInfo("You can now login using your Student ID and password.");
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
     * Handle student login
     */
    public void handleStudentLogin() {
        try {
            InputUtils.displayHeader("STUDENT LOGIN");
            
            String studentId = InputUtils.readString("Student ID");
            String password = InputUtils.readPassword("Password", 1);
            
            // Authenticate student
            Student student = studentService.authenticateStudent(studentId, password);
            
            InputUtils.displaySuccess("Login successful! Welcome, " + student.getFullName());
            
            // Start student session
            runStudentSession(student);
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Login failed: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Run student session after successful login
     */
    private void runStudentSession(Student student) {
        while (true) {
            try {
                int choice = showStudentMenu(student);
                
                switch (choice) {
                    case 1 -> viewMyProfile(student);
                    case 2 -> applyForLeave(student);
                    case 3 -> viewMyLeaves(student);
                    case 4 -> cancelLeave(student);
                    case 5 -> viewLeaveHistory(student);
                    case 6 -> updateProfile(student);
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
     * Show student menu
     */
    private int showStudentMenu(Student student) {
        String[] options = {
            "View My Profile",
            "Apply for Leave",
            "View My Leaves",
            "Cancel Leave",
            "View Leave History",
            "Update Profile"
        };
        
        return InputUtils.readMenuChoice("STUDENT DASHBOARD - " + student.getFullName(), options);
    }

    /**
     * View student profile
     */
    private void viewMyProfile(Student student) {
        InputUtils.displayHeader("MY PROFILE");
        
        System.out.println("Student Information:");
        System.out.println("  Student ID: " + student.getStudentId());
        System.out.println("  Name: " + student.getFullName());
        System.out.println("  Email: " + student.getEmail());
        System.out.println("  Phone: " + (student.getPhoneNumber() != null ? student.getPhoneNumber() : "Not provided"));
        System.out.println("  Department: " + student.getDepartment());
        System.out.println("  Year: " + student.getYear());
        System.out.println("  Status: " + (student.isActive() ? "Active" : "Inactive"));
        System.out.println("  Member Since: " + student.getCreatedAt().toLocalDate());
        
        InputUtils.waitForEnter();
    }

    /**
     * Apply for leave
     */
    private void applyForLeave(Student student) {
        try {
            InputUtils.displayHeader("APPLY FOR LEAVE");
            
            // Show leave types
            System.out.println("Available Leave Types:");
            LeaveType[] types = LeaveType.values();
            for (int i = 0; i < types.length; i++) {
                System.out.printf("%d. %s%n", i + 1, types[i].getDisplayName());
            }
            
            int typeChoice = InputUtils.readInt("Select leave type", 1, types.length);
            LeaveType leaveType = types[typeChoice - 1];
            
            // Get leave dates
            LocalDate startDate = InputUtils.readDateAfter("Start Date", LocalDate.now());
            LocalDate endDate = InputUtils.readDateAfter("End Date", startDate.minusDays(1));
            
            // Calculate duration
            long duration = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            System.out.printf("Leave Duration: %d days%n", duration);
            
            // Get reason
            String reason = InputUtils.readString("Reason for leave", 10);
            
            // Confirm application
            System.out.println("\nLeave Application Summary:");
            System.out.println("  Type: " + leaveType.getDisplayName());
            System.out.println("  Duration: " + startDate + " to " + endDate + " (" + duration + " days)");
            System.out.println("  Reason: " + reason);
            
            boolean confirm = InputUtils.readBoolean("Confirm application");
            
            if (confirm) {
                // Create leave object
                Leave leave = new Leave(student.getStudentId(), leaveType, startDate, endDate, reason);
                
                // Apply for leave
                Long leaveId = leaveService.applyForLeave(leave);
                
                InputUtils.displaySuccess("Leave application submitted successfully!");
                InputUtils.displayInfo("Your leave request ID is: " + leaveId);
                InputUtils.displayInfo("Your leave request is pending approval from the admin.");
            } else {
                InputUtils.displayInfo("Leave application cancelled.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to apply for leave: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * View student's leaves
     */
    private void viewMyLeaves(Student student) {
        try {
            InputUtils.displayHeader("MY LEAVES");
            
            List<Leave> leaves = leaveService.getLeavesByStudentId(student.getStudentId());
            
            if (leaves.isEmpty()) {
                InputUtils.displayInfo("You have no leave applications.");
            } else {
                String[] headers = {"ID", "Type", "Start Date", "End Date", "Duration", "Status", "Applied On"};
                String[][] data = new String[leaves.size()][7];
                
                for (int i = 0; i < leaves.size(); i++) {
                    Leave leave = leaves.get(i);
                    data[i][0] = String.valueOf(leave.getLeaveId());
                    data[i][1] = leave.getLeaveType().getDisplayName();
                    data[i][2] = leave.getStartDate().toString();
                    data[i][3] = leave.getEndDate().toString();
                    data[i][4] = leave.getDurationInDays() + " days";
                    data[i][5] = leave.getStatus().getDisplayName();
                    data[i][6] = leave.getCreatedAt().toLocalDate().toString();
                }
                
                InputUtils.displayTable(headers, data);
                
                // Show details option
                if (InputUtils.readBoolean("View details of a specific leave")) {
                    Long leaveId = InputUtils.readLong("Enter Leave ID");
                    viewLeaveDetails(leaveId);
                }
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to fetch leaves: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * Cancel a leave
     */
    private void cancelLeave(Student student) {
        try {
            InputUtils.displayHeader("CANCEL LEAVE");
            
            // First show current leaves that can be cancelled
            List<Leave> leaves = leaveService.getLeavesByStudentId(student.getStudentId());
            List<Leave> cancellableLeaves = leaves.stream()
                .filter(Leave::canBeCancelled)
                .toList();
            
            if (cancellableLeaves.isEmpty()) {
                InputUtils.displayInfo("You have no leaves that can be cancelled.");
                InputUtils.waitForEnter();
                return;
            }
            
            System.out.println("Leaves that can be cancelled:");
            String[] headers = {"ID", "Type", "Start Date", "End Date", "Status"};
            String[][] data = new String[cancellableLeaves.size()][5];
            
            for (int i = 0; i < cancellableLeaves.size(); i++) {
                Leave leave = cancellableLeaves.get(i);
                data[i][0] = String.valueOf(leave.getLeaveId());
                data[i][1] = leave.getLeaveType().getDisplayName();
                data[i][2] = leave.getStartDate().toString();
                data[i][3] = leave.getEndDate().toString();
                data[i][4] = leave.getStatus().getDisplayName();
            }
            
            InputUtils.displayTable(headers, data);
            
            Long leaveId = InputUtils.readLong("Enter Leave ID to cancel");
            
            // Verify the leave belongs to this student and can be cancelled
            boolean validLeave = cancellableLeaves.stream()
                .anyMatch(leave -> leave.getLeaveId().equals(leaveId));
            
            if (!validLeave) {
                InputUtils.displayError("Invalid leave ID or leave cannot be cancelled.");
                InputUtils.waitForEnter();
                return;
            }
            
            boolean confirm = InputUtils.readBoolean("Are you sure you want to cancel this leave");
            
            if (confirm) {
                boolean success = leaveService.cancelLeave(leaveId, student.getStudentId());
                
                if (success) {
                    InputUtils.displaySuccess("Leave cancelled successfully!");
                } else {
                    InputUtils.displayError("Failed to cancel leave. Please try again.");
                }
            } else {
                InputUtils.displayInfo("Leave cancellation aborted.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to cancel leave: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * View leave history
     */
    private void viewLeaveHistory(Student student) {
        try {
            InputUtils.displayHeader("LEAVE HISTORY");
            
            List<Leave> leaves = leaveService.getLeavesByStudentId(student.getStudentId());
            
            if (leaves.isEmpty()) {
                InputUtils.displayInfo("You have no leave history.");
            } else {
                // Show summary statistics
                long totalLeaves = leaves.size();
                long approvedLeaves = leaves.stream().mapToLong(l -> l.getStatus() == Leave.LeaveStatus.APPROVED ? 1 : 0).sum();
                long pendingLeaves = leaves.stream().mapToLong(l -> l.getStatus() == Leave.LeaveStatus.PENDING ? 1 : 0).sum();
                long rejectedLeaves = leaves.stream().mapToLong(l -> l.getStatus() == Leave.LeaveStatus.REJECTED ? 1 : 0).sum();
                
                System.out.println("Leave Statistics:");
                System.out.println("  Total Applications: " + totalLeaves);
                System.out.println("  Approved: " + approvedLeaves);
                System.out.println("  Pending: " + pendingLeaves);
                System.out.println("  Rejected: " + rejectedLeaves);
                System.out.println();
                
                // Show detailed history
                viewMyLeaves(student);
            }
            
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
            InputUtils.waitForEnter();
        }
    }

    /**
     * Update student profile
     */
    private void updateProfile(Student student) {
        try {
            InputUtils.displayHeader("UPDATE PROFILE");
            
            System.out.println("Current Information:");
            System.out.println("  Name: " + student.getFullName());
            System.out.println("  Email: " + student.getEmail());
            System.out.println("  Phone: " + (student.getPhoneNumber() != null ? student.getPhoneNumber() : "Not provided"));
            System.out.println("  Department: " + student.getDepartment());
            System.out.println("  Year: " + student.getYear());
            System.out.println();
            
            if (InputUtils.readBoolean("Do you want to update your profile")) {
                // Update fields (keeping current values as defaults)
                String firstName = InputUtils.readString("First Name (current: " + student.getFirstName() + ")");
                String lastName = InputUtils.readString("Last Name (current: " + student.getLastName() + ")");
                String email = InputUtils.readEmail("Email (current: " + student.getEmail() + ")");
                String phoneNumber = InputUtils.readOptionalString("Phone Number (current: " + 
                    (student.getPhoneNumber() != null ? student.getPhoneNumber() : "Not provided") + ")");
                String department = InputUtils.readString("Department (current: " + student.getDepartment() + ")");
                String year = InputUtils.readString("Year (current: " + student.getYear() + ")");
                
                // Update student object
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                student.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
                student.setDepartment(department);
                student.setYear(year);
                
                // Save updates
                boolean success = studentService.updateStudent(student);
                
                if (success) {
                    InputUtils.displaySuccess("Profile updated successfully!");
                } else {
                    InputUtils.displayError("Failed to update profile. Please try again.");
                }
            } else {
                InputUtils.displayInfo("Profile update cancelled.");
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to update profile: " + e.getMessage());
        } catch (Exception e) {
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
        
        InputUtils.waitForEnter();
    }

    /**
     * View details of a specific leave
     */
    private void viewLeaveDetails(Long leaveId) {
        try {
            Leave leave = leaveService.getLeaveById(leaveId);
            
            System.out.println("\nLeave Details:");
            System.out.println("  Leave ID: " + leave.getLeaveId());
            System.out.println("  Type: " + leave.getLeaveType().getDisplayName());
            System.out.println("  Start Date: " + leave.getStartDate());
            System.out.println("  End Date: " + leave.getEndDate());
            System.out.println("  Duration: " + leave.getDurationInDays() + " days");
            System.out.println("  Status: " + leave.getStatus().getDisplayName());
            System.out.println("  Reason: " + leave.getReason());
            System.out.println("  Applied On: " + leave.getCreatedAt().toLocalDate());
            
            if (leave.getAdminComments() != null && !leave.getAdminComments().trim().isEmpty()) {
                System.out.println("  Admin Comments: " + leave.getAdminComments());
            }
            
            if (leave.getApprovedBy() != null) {
                System.out.println("  Processed By: " + leave.getApprovedBy());
                System.out.println("  Processed On: " + leave.getApprovedAt().toLocalDate());
            }
            
        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to fetch leave details: " + e.getMessage());
        }
    }
}
