package com.studentleave.services;

import com.studentleave.database.DatabaseConnection;
import com.studentleave.exceptions.LeaveManagementException;
import com.studentleave.models.Leave;
import com.studentleave.models.Student;
import com.studentleave.utils.InputUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for generating reports and analytics
 * Demonstrates data analysis and reporting functionality
 */
public class ReportingService {
    private static final Logger logger = Logger.getLogger(ReportingService.class.getName());
    private final LeaveService leaveService;
    private final StudentService studentService;

    public ReportingService() {
        this.leaveService = new LeaveService();
        this.studentService = new StudentService();
    }

    /**
     * Generate department-wise leave statistics
     */
    public void generateDepartmentStatistics() {
        try {
            InputUtils.displayHeader("DEPARTMENT-WISE LEAVE STATISTICS");

            String sql = """
                SELECT 
                    s.department,
                    COUNT(*) as total_leaves,
                    SUM(CASE WHEN l.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_leaves,
                    SUM(CASE WHEN l.status = 'PENDING' THEN 1 ELSE 0 END) as pending_leaves,
                    SUM(CASE WHEN l.status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_leaves,
                    AVG(DATEDIFF(l.end_date, l.start_date) + 1) as avg_duration,
                    SUM(CASE WHEN l.status = 'APPROVED' THEN DATEDIFF(l.end_date, l.start_date) + 1 ELSE 0 END) as total_approved_days
                FROM leaves l
                JOIN students s ON l.student_id = s.student_id
                WHERE l.created_at >= DATE_SUB(CURDATE(), INTERVAL 1 YEAR)
                GROUP BY s.department
                ORDER BY total_leaves DESC
            """;

            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                try (PreparedStatement stmt = connection.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    System.out.println("Leave Statistics by Department (Last 12 Months):");
                    System.out.println();

                    String[] headers = {"Department", "Total", "Approved", "Pending", "Rejected", 
                                      "Avg Duration", "Total Days"};
                    List<String[]> dataList = new ArrayList<>();

                    int totalLeaves = 0;
                    int totalApproved = 0;
                    int totalPending = 0;
                    int totalRejected = 0;
                    double totalApprovedDays = 0;

                    while (rs.next()) {
                        String[] row = new String[7];
                        row[0] = rs.getString("department");
                        row[1] = String.valueOf(rs.getInt("total_leaves"));
                        row[2] = String.valueOf(rs.getInt("approved_leaves"));
                        row[3] = String.valueOf(rs.getInt("pending_leaves"));
                        row[4] = String.valueOf(rs.getInt("rejected_leaves"));
                        row[5] = String.format("%.1f days", rs.getDouble("avg_duration"));
                        row[6] = String.valueOf(rs.getInt("total_approved_days"));

                        dataList.add(row);

                        totalLeaves += rs.getInt("total_leaves");
                        totalApproved += rs.getInt("approved_leaves");
                        totalPending += rs.getInt("pending_leaves");
                        totalRejected += rs.getInt("rejected_leaves");
                        totalApprovedDays += rs.getInt("total_approved_days");
                    }

                    if (!dataList.isEmpty()) {
                        String[][] data = dataList.toArray(new String[0][]);
                        InputUtils.displayTable(headers, data);

                        // Summary statistics
                        System.out.println("\nOverall Summary:");
                        System.out.println("  Total Leave Applications: " + totalLeaves);
                        System.out.println("  Approved: " + totalApproved + " (" + 
                            (totalLeaves > 0 ? String.format("%.1f%%", (totalApproved * 100.0 / totalLeaves)) : "0%") + ")");
                        System.out.println("  Pending: " + totalPending + " (" + 
                            (totalLeaves > 0 ? String.format("%.1f%%", (totalPending * 100.0 / totalLeaves)) : "0%") + ")");
                        System.out.println("  Rejected: " + totalRejected + " (" + 
                            (totalLeaves > 0 ? String.format("%.1f%%", (totalRejected * 100.0 / totalLeaves)) : "0%") + ")");
                        System.out.println("  Total Approved Leave Days: " + (int)totalApprovedDays);
                    } else {
                        InputUtils.displayInfo("No leave data found for the last 12 months.");
                    }
                }
            } finally {
                DatabaseConnection.returnConnection(connection);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during report generation", e);
            InputUtils.displayError("Failed to generate department statistics: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during report generation", e);
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Generate monthly leave summary report
     */
    public void generateMonthlyReport() {
        try {
            InputUtils.displayHeader("MONTHLY LEAVE SUMMARY");

            // Get year and month from user
            int currentYear = LocalDate.now().getYear();
            int year = InputUtils.readInt("Enter year (e.g., " + currentYear + ")", 2020, 2030);
            int month = InputUtils.readInt("Enter month (1-12)", 1, 12);

            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            String sql = """
                SELECT 
                    DATE(l.created_at) as application_date,
                    l.leave_type,
                    COUNT(*) as applications_count,
                    SUM(CASE WHEN l.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_count,
                    SUM(CASE WHEN l.status = 'APPROVED' THEN DATEDIFF(l.end_date, l.start_date) + 1 ELSE 0 END) as approved_days
                FROM leaves l
                WHERE DATE(l.created_at) BETWEEN ? AND ?
                GROUP BY DATE(l.created_at), l.leave_type
                ORDER BY DATE(l.created_at) DESC, l.leave_type
            """;

            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setDate(1, java.sql.Date.valueOf(startDate));
                    stmt.setDate(2, java.sql.Date.valueOf(endDate));

                    try (ResultSet rs = stmt.executeQuery()) {
                        System.out.println("Monthly Leave Summary for " + yearMonth.getMonth() + " " + year);
                        System.out.println();

                        String[] headers = {"Date", "Leave Type", "Applications", "Approved", "Approved Days"};
                        List<String[]> dataList = new ArrayList<>();

                        int totalApplications = 0;
                        int totalApproved = 0;
                        int totalDays = 0;

                        while (rs.next()) {
                            String[] row = new String[5];
                            row[0] = rs.getDate("application_date").toString();
                            row[1] = rs.getString("leave_type");
                            row[2] = String.valueOf(rs.getInt("applications_count"));
                            row[3] = String.valueOf(rs.getInt("approved_count"));
                            row[4] = String.valueOf(rs.getInt("approved_days"));

                            dataList.add(row);

                            totalApplications += rs.getInt("applications_count");
                            totalApproved += rs.getInt("approved_count");
                            totalDays += rs.getInt("approved_days");
                        }

                        if (!dataList.isEmpty()) {
                            String[][] data = dataList.toArray(new String[0][]);
                            InputUtils.displayTable(headers, data);

                            System.out.println("\nMonthly Summary:");
                            System.out.println("  Total Applications: " + totalApplications);
                            System.out.println("  Total Approved: " + totalApproved);
                            System.out.println("  Total Approved Days: " + totalDays);
                            System.out.println("  Approval Rate: " + 
                                (totalApplications > 0 ? String.format("%.1f%%", (totalApproved * 100.0 / totalApplications)) : "0%"));
                        } else {
                            InputUtils.displayInfo("No leave applications found for " + yearMonth.getMonth() + " " + year);
                        }
                    }
                }
            } finally {
                DatabaseConnection.returnConnection(connection);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during monthly report generation", e);
            InputUtils.displayError("Failed to generate monthly report: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during monthly report generation", e);
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Generate detailed report for a specific student
     */
    public void generateStudentReport(String studentId) {
        try {
            InputUtils.displayHeader("STUDENT LEAVE REPORT");

            // Get student details
            Student student = studentService.getStudentById(studentId);
            
            System.out.println("Student Information:");
            System.out.println("  ID: " + student.getStudentId());
            System.out.println("  Name: " + student.getFullName());
            System.out.println("  Department: " + student.getDepartment());
            System.out.println("  Year: " + student.getYear());
            System.out.println("  Email: " + student.getEmail());
            System.out.println();

            // Get all leaves for the student
            List<Leave> leaves = leaveService.getLeavesByStudentId(studentId);

            if (leaves.isEmpty()) {
                InputUtils.displayInfo("No leave applications found for this student.");
                return;
            }

            // Calculate statistics
            Map<Leave.LeaveStatus, Integer> statusCounts = new HashMap<>();
            Map<Leave.LeaveType, Integer> typeCounts = new HashMap<>();
            int totalDays = 0;
            int approvedDays = 0;

            for (Leave leave : leaves) {
                statusCounts.put(leave.getStatus(), statusCounts.getOrDefault(leave.getStatus(), 0) + 1);
                typeCounts.put(leave.getLeaveType(), typeCounts.getOrDefault(leave.getLeaveType(), 0) + 1);
                totalDays += leave.getDurationInDays();
                if (leave.getStatus() == Leave.LeaveStatus.APPROVED) {
                    approvedDays += leave.getDurationInDays();
                }
            }

            // Display statistics
            System.out.println("Leave Statistics:");
            System.out.println("  Total Applications: " + leaves.size());
            for (Leave.LeaveStatus status : statusCounts.keySet()) {
                System.out.println("  " + status.getDisplayName() + ": " + statusCounts.get(status));
            }
            System.out.println("  Total Leave Days Applied: " + totalDays);
            System.out.println("  Total Approved Days: " + approvedDays);
            System.out.println();

            // Display leaves by type
            System.out.println("Leave Applications by Type:");
            for (Leave.LeaveType type : typeCounts.keySet()) {
                System.out.println("  " + type.getDisplayName() + ": " + typeCounts.get(type));
            }
            System.out.println();

            // Display detailed leave history
            System.out.println("Detailed Leave History:");
            String[] headers = {"ID", "Type", "Start Date", "End Date", "Duration", "Status", "Applied On", "Processed On"};
            String[][] data = new String[leaves.size()][8];

            for (int i = 0; i < leaves.size(); i++) {
                Leave leave = leaves.get(i);
                data[i][0] = String.valueOf(leave.getLeaveId());
                data[i][1] = leave.getLeaveType().getDisplayName();
                data[i][2] = leave.getStartDate().toString();
                data[i][3] = leave.getEndDate().toString();
                data[i][4] = leave.getDurationInDays() + " days";
                data[i][5] = leave.getStatus().getDisplayName();
                data[i][6] = leave.getCreatedAt().toLocalDate().toString();
                data[i][7] = leave.getApprovedAt() != null ? leave.getApprovedAt().toLocalDate().toString() : "N/A";
            }

            InputUtils.displayTable(headers, data);

            // Show recent leave details if any
            if (!leaves.isEmpty() && InputUtils.readBoolean("View details of most recent leave")) {
                Leave recentLeave = leaves.get(0); // Assuming sorted by created date desc
                displayLeaveDetails(recentLeave, student);
            }

        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to generate student report: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during student report generation", e);
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Generate report of pending approval requests
     */
    public void generatePendingApprovalsReport() {
        try {
            InputUtils.displayHeader("PENDING APPROVALS REPORT");

            List<Leave> pendingLeaves = leaveService.getPendingLeaves();

            if (pendingLeaves.isEmpty()) {
                InputUtils.displayInfo("No pending leave approvals found.");
                return;
            }

            System.out.println("Pending Leave Approvals Summary:");
            System.out.println("  Total Pending: " + pendingLeaves.size());
            System.out.println();

            // Categorize by urgency (how soon the leave starts)
            LocalDate today = LocalDate.now();
            List<Leave> urgent = new ArrayList<>();    // Starts within 3 days
            List<Leave> medium = new ArrayList<>();    // Starts within 7 days
            List<Leave> normal = new ArrayList<>();    // Starts later

            for (Leave leave : pendingLeaves) {
                long daysUntilStart = today.until(leave.getStartDate()).getDays();
                if (daysUntilStart <= 3) {
                    urgent.add(leave);
                } else if (daysUntilStart <= 7) {
                    medium.add(leave);
                } else {
                    normal.add(leave);
                }
            }

            System.out.println("Priority Breakdown:");
            System.out.println("  Urgent (starts within 3 days): " + urgent.size());
            System.out.println("  Medium (starts within 7 days): " + medium.size());
            System.out.println("  Normal (starts later): " + normal.size());
            System.out.println();

            // Display all pending leaves with student information
            String[] headers = {"ID", "Student", "Department", "Type", "Start Date", "Duration", "Days Until", "Applied On"};
            String[][] data = new String[pendingLeaves.size()][8];

            for (int i = 0; i < pendingLeaves.size(); i++) {
                Leave leave = pendingLeaves.get(i);
                try {
                    Student student = studentService.getStudentById(leave.getStudentId());
                    long daysUntil = today.until(leave.getStartDate()).getDays();
                    
                    data[i][0] = String.valueOf(leave.getLeaveId());
                    data[i][1] = student.getFullName();
                    data[i][2] = student.getDepartment();
                    data[i][3] = leave.getLeaveType().getDisplayName();
                    data[i][4] = leave.getStartDate().toString();
                    data[i][5] = leave.getDurationInDays() + " days";
                    data[i][6] = daysUntil > 0 ? daysUntil + " days" : 
                                (daysUntil == 0 ? "Today" : "Overdue");
                    data[i][7] = leave.getCreatedAt().toLocalDate().toString();
                } catch (LeaveManagementException e) {
                    data[i][1] = "Unknown Student";
                    data[i][2] = "Unknown";
                }
            }

            InputUtils.displayTable(headers, data);

            // Show department-wise breakdown
            if (InputUtils.readBoolean("View department-wise breakdown")) {
                generatePendingByDepartment(pendingLeaves);
            }

        } catch (LeaveManagementException e) {
            InputUtils.displayError("Failed to generate pending approvals report: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during pending approvals report generation", e);
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Generate trend analysis report
     */
    public void generateTrendAnalysis() {
        try {
            InputUtils.displayHeader("LEAVE TRENDS ANALYSIS");

            String sql = """
                SELECT 
                    YEAR(l.created_at) as year,
                    MONTH(l.created_at) as month,
                    l.leave_type,
                    COUNT(*) as count,
                    AVG(DATEDIFF(l.end_date, l.start_date) + 1) as avg_duration
                FROM leaves l
                WHERE l.created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
                GROUP BY YEAR(l.created_at), MONTH(l.created_at), l.leave_type
                ORDER BY year DESC, month DESC, l.leave_type
            """;

            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                try (PreparedStatement stmt = connection.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    System.out.println("Leave Trends (Last 12 Months):");
                    System.out.println();

                    String[] headers = {"Year-Month", "Leave Type", "Count", "Avg Duration"};
                    List<String[]> dataList = new ArrayList<>();

                    while (rs.next()) {
                        String[] row = new String[4];
                        row[0] = rs.getInt("year") + "-" + String.format("%02d", rs.getInt("month"));
                        row[1] = rs.getString("leave_type");
                        row[2] = String.valueOf(rs.getInt("count"));
                        row[3] = String.format("%.1f days", rs.getDouble("avg_duration"));

                        dataList.add(row);
                    }

                    if (!dataList.isEmpty()) {
                        String[][] data = dataList.toArray(new String[0][]);
                        InputUtils.displayTable(headers, data);
                    } else {
                        InputUtils.displayInfo("No trend data available for the last 12 months.");
                    }
                }
            } finally {
                DatabaseConnection.returnConnection(connection);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during trend analysis", e);
            InputUtils.displayError("Failed to generate trend analysis: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during trend analysis", e);
            InputUtils.displayError("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Helper methods
    private void generatePendingByDepartment(List<Leave> pendingLeaves) {
        try {
            Map<String, Integer> departmentCounts = new HashMap<>();
            
            for (Leave leave : pendingLeaves) {
                try {
                    Student student = studentService.getStudentById(leave.getStudentId());
                    departmentCounts.put(student.getDepartment(), 
                        departmentCounts.getOrDefault(student.getDepartment(), 0) + 1);
                } catch (LeaveManagementException e) {
                    departmentCounts.put("Unknown", 
                        departmentCounts.getOrDefault("Unknown", 0) + 1);
                }
            }

            System.out.println("\nPending Leaves by Department:");
            String[] headers = {"Department", "Pending Count"};
            String[][] data = new String[departmentCounts.size()][2];
            
            int index = 0;
            for (Map.Entry<String, Integer> entry : departmentCounts.entrySet()) {
                data[index][0] = entry.getKey();
                data[index][1] = String.valueOf(entry.getValue());
                index++;
            }

            InputUtils.displayTable(headers, data);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error generating department breakdown", e);
            InputUtils.displayError("Failed to generate department breakdown: " + e.getMessage());
        }
    }

    private void displayLeaveDetails(Leave leave, Student student) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DETAILED LEAVE INFORMATION");
        System.out.println("=".repeat(50));
        System.out.println("Leave ID: " + leave.getLeaveId());
        System.out.println("Student: " + student.getFullName() + " (" + student.getStudentId() + ")");
        System.out.println("Department: " + student.getDepartment());
        System.out.println("Year: " + student.getYear());
        System.out.println("Leave Type: " + leave.getLeaveType().getDisplayName());
        System.out.println("Duration: " + leave.getStartDate() + " to " + leave.getEndDate() + 
                         " (" + leave.getDurationInDays() + " days)");
        System.out.println("Reason: " + leave.getReason());
        System.out.println("Status: " + leave.getStatus().getDisplayName());
        System.out.println("Applied On: " + leave.getCreatedAt().toLocalDate());
        
        if (leave.getAdminComments() != null && !leave.getAdminComments().trim().isEmpty()) {
            System.out.println("Admin Comments: " + leave.getAdminComments());
        }
        
        if (leave.getApprovedBy() != null) {
            System.out.println("Processed By: " + leave.getApprovedBy());
            System.out.println("Processed On: " + leave.getApprovedAt().toLocalDate());
        }
        
        System.out.println("=".repeat(50));
    }
}
