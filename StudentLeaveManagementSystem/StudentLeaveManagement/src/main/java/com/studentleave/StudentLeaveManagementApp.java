package com.studentleave;

import com.studentleave.controllers.AdminController;
import com.studentleave.controllers.StudentController;
import com.studentleave.database.DatabaseConnection;
import com.studentleave.utils.InputUtils;

/**
 * Main application class for Student Leave Management System
 * Entry point for the console-based application
 */
public class StudentLeaveManagementApp {
    private static StudentController studentController;
    private static AdminController adminController;

    public static void main(String[] args) {
        try {
            // Initialize controllers
            studentController = new StudentController();
            adminController = new AdminController();
            
            // Display welcome message
            displayWelcome();
            
            // Test database connection
            if (!testDatabaseConnection()) {
                InputUtils.displayError("Cannot connect to database. Please check your configuration.");
                return;
            }
            
            // Main application loop
            runApplication();
            
        } catch (Exception e) {
            InputUtils.displayError("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup resources
            cleanup();
        }
    }
    
    /**
     * Display welcome message and system information
     */
    private static void displayWelcome() {
        InputUtils.clearConsole();
        InputUtils.displayHeader("STUDENT LEAVE MANAGEMENT SYSTEM");
        System.out.println("Welcome to the Student Leave Management System!");
        System.out.println("This system helps manage student leave requests efficiently.");
        System.out.println("Version: 1.0.0");
        System.out.println("Developed using Java with JDBC and MySQL");
        InputUtils.displaySeparator();
    }
    
    /**
     * Test database connectivity
     */
    private static boolean testDatabaseConnection() {
        InputUtils.displayInfo("Testing database connection...");
        boolean connected = DatabaseConnection.testConnection();
        
        if (connected) {
            InputUtils.displaySuccess("Database connection successful!");
            System.out.println("Database URL: " + DatabaseConnection.getDatabaseUrl());
            System.out.println("Pool Size: " + DatabaseConnection.getPoolSize());
        } else {
            InputUtils.displayError("Database connection failed!");
            System.out.println("Please ensure MySQL is running and database is properly configured.");
        }
        
        return connected;
    }
    
    /**
     * Main application loop
     */
    private static void runApplication() {
        while (true) {
            try {
                int choice = showMainMenu();
                
                switch (choice) {
                    case 1 -> handleStudentPortal();
                    case 2 -> handleAdminPortal();
                    case 3 -> showSystemInformation();
                    case 4 -> showAbout();
                    case 0 -> {
                        InputUtils.displayInfo("Thank you for using Student Leave Management System!");
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
     * Show main menu and get user choice
     */
    private static int showMainMenu() {
        String[] options = {
            "Student Portal",
            "Admin Portal", 
            "System Information",
            "About"
        };
        
        return InputUtils.readMenuChoice("MAIN MENU", options);
    }
    
    /**
     * Handle student portal access
     */
    private static void handleStudentPortal() {
        InputUtils.displayHeader("STUDENT PORTAL");
        
        String[] options = {
            "Student Login",
            "Student Registration"
        };
        
        int choice = InputUtils.readMenuChoice("Student Portal", options);
        
        switch (choice) {
            case 1 -> studentController.handleStudentLogin();
            case 2 -> studentController.handleStudentRegistration();
            case 0 -> {} // Return to main menu
            default -> InputUtils.displayWarning("Invalid choice.");
        }
    }
    
    /**
     * Handle admin portal access
     */
    private static void handleAdminPortal() {
        InputUtils.displayHeader("ADMIN PORTAL");
        
        String[] options = {
            "Admin Login",
            "Admin Registration (Super Admin Only)"
        };
        
        int choice = InputUtils.readMenuChoice("Admin Portal", options);
        
        switch (choice) {
            case 1 -> adminController.handleAdminLogin();
            case 2 -> adminController.handleAdminRegistration();
            case 0 -> {} // Return to main menu
            default -> InputUtils.displayWarning("Invalid choice.");
        }
    }
    
    /**
     * Show system information
     */
    private static void showSystemInformation() {
        InputUtils.displayHeader("SYSTEM INFORMATION");
        
        System.out.println("Application Details:");
        System.out.println("  Name: Student Leave Management System");
        System.out.println("  Version: 1.0.0");
        System.out.println("  Language: Java");
        System.out.println("  Database: MySQL");
        System.out.println("  Architecture: MVC with Service Layer");
        System.out.println();
        
        System.out.println("Database Information:");
        System.out.println("  URL: " + DatabaseConnection.getDatabaseUrl());
        System.out.println("  Connection Pool Size: " + DatabaseConnection.getPoolSize());
        System.out.println("  Active Connections: " + DatabaseConnection.getActiveConnectionCount());
        System.out.println();
        
        System.out.println("System Features:");
        System.out.println("  ✓ Student Registration and Authentication");
        System.out.println("  ✓ Admin Role-based Access Control");
        System.out.println("  ✓ Leave Application and Management");
        System.out.println("  ✓ Leave Approval Workflow");
        System.out.println("  ✓ Comprehensive Reporting");
        System.out.println("  ✓ Data Validation and Error Handling");
        System.out.println("  ✓ Audit Trail and History Tracking");
        System.out.println();
        
        System.out.println("Technical Highlights:");
        System.out.println("  ✓ Object-Oriented Programming (OOP)");
        System.out.println("  ✓ JDBC Database Integration");
        System.out.println("  ✓ Connection Pooling");
        System.out.println("  ✓ Custom Exception Handling");
        System.out.println("  ✓ Input Validation and Sanitization");
        System.out.println("  ✓ Modular Design Patterns");
        System.out.println("  ✓ Business Logic Separation");
        System.out.println("  ✓ Comprehensive Logging");
        
        InputUtils.waitForEnter();
    }
    
    /**
     * Show about information
     */
    private static void showAbout() {
        InputUtils.displayHeader("ABOUT");
        
        System.out.println("Student Leave Management System");
        System.out.println("==============================");
        System.out.println();
        System.out.println("This application is designed to demonstrate comprehensive Java");
        System.out.println("programming skills including:");
        System.out.println();
        System.out.println("Core Java Concepts:");
        System.out.println("  • Object-Oriented Programming (OOP)");
        System.out.println("  • Encapsulation, Inheritance, and Polymorphism");
        System.out.println("  • Abstract Classes and Interfaces");
        System.out.println("  • Exception Handling");
        System.out.println("  • Collections Framework");
        System.out.println("  • Generics and Enums");
        System.out.println();
        System.out.println("Database Integration:");
        System.out.println("  • JDBC Connectivity");
        System.out.println("  • PreparedStatement Usage");
        System.out.println("  • Connection Pooling");
        System.out.println("  • Transaction Management");
        System.out.println();
        System.out.println("Design Patterns:");
        System.out.println("  • Model-View-Controller (MVC)");
        System.out.println("  • Service Layer Pattern");
        System.out.println("  • Data Access Object (DAO) Pattern");
        System.out.println("  • Singleton Pattern");
        System.out.println();
        System.out.println("Best Practices:");
        System.out.println("  • Input Validation");
        System.out.println("  • Error Handling");
        System.out.println("  • Code Documentation");
        System.out.println("  • Modular Design");
        System.out.println("  • Security Considerations");
        System.out.println();
        System.out.println("This project showcases practical Java backend development");
        System.out.println("skills suitable for enterprise-level applications.");
        
        InputUtils.waitForEnter();
    }
    
    /**
     * Cleanup resources before application exit
     */
    private static void cleanup() {
        try {
            // Shutdown database connection pool
            DatabaseConnection.shutdown();
            
            // Close input scanner
            InputUtils.closeScanner();
            
            System.out.println("Resources cleaned up successfully.");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}
