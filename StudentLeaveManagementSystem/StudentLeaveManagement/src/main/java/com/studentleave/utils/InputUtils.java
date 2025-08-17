package com.studentleave.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Utility class for handling console input with validation
 * Demonstrates utility design patterns and input validation
 */
public class InputUtils {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Read a non-empty string from console
     */
    public static String readString(String prompt) {
        System.out.print(prompt + ": ");
        String input = scanner.nextLine().trim();
        while (input.isEmpty()) {
            System.out.print("Input cannot be empty. " + prompt + ": ");
            input = scanner.nextLine().trim();
        }
        return input;
    }

    /**
     * Read a string with minimum length from console
     */
    public static String readString(String prompt, int minLength) {
        String input = readString(prompt);
        while (input.length() < minLength) {
            System.out.printf("Input must be at least %d characters long. %s: ", minLength, prompt);
            input = scanner.nextLine().trim();
        }
        return input;
    }

    /**
     * Read an optional string from console
     */
    public static String readOptionalString(String prompt) {
        System.out.print(prompt + " (optional): ");
        return scanner.nextLine().trim();
    }

    /**
     * Read an integer from console
     */
    public static int readInt(String prompt) {
        System.out.print(prompt + ": ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid integer. " + prompt + ": ");
            }
        }
    }

    /**
     * Read an integer within a range from console
     */
    public static int readInt(String prompt, int min, int max) {
        int value = readInt(prompt);
        while (value < min || value > max) {
            System.out.printf("Please enter a value between %d and %d. %s: ", min, max, prompt);
            try {
                String input = scanner.nextLine().trim();
                value = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                value = min - 1; // Force retry
            }
        }
        return value;
    }

    /**
     * Read a long from console
     */
    public static long readLong(String prompt) {
        System.out.print(prompt + ": ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number. " + prompt + ": ");
            }
        }
    }

    /**
     * Read a boolean from console (y/n)
     */
    public static boolean readBoolean(String prompt) {
        System.out.print(prompt + " (y/n): ");
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            } else {
                System.out.print("Please enter 'y' or 'n'. " + prompt + " (y/n): ");
            }
        }
    }

    /**
     * Read a date from console in YYYY-MM-DD format
     */
    public static LocalDate readDate(String prompt) {
        System.out.print(prompt + " (YYYY-MM-DD): ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.print("Please enter date in YYYY-MM-DD format. " + prompt + " (YYYY-MM-DD): ");
            }
        }
    }

    /**
     * Read a date that must be after a certain date
     */
    public static LocalDate readDateAfter(String prompt, LocalDate afterDate) {
        LocalDate date = readDate(prompt);
        while (!date.isAfter(afterDate)) {
            System.out.printf("Date must be after %s. %s (YYYY-MM-DD): ", 
                afterDate.format(DATE_FORMATTER), prompt);
            try {
                String input = scanner.nextLine().trim();
                date = LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                date = afterDate; // Force retry
            }
        }
        return date;
    }

    /**
     * Read email with validation
     */
    public static String readEmail(String prompt) {
        String email = readString(prompt);
        while (!isValidEmail(email)) {
            System.out.print("Please enter a valid email address. " + prompt + ": ");
            email = scanner.nextLine().trim();
        }
        return email;
    }

    /**
     * Read password with minimum length requirement
     */
    public static String readPassword(String prompt, int minLength) {
        System.out.print(prompt + ": ");
        String password = scanner.nextLine();
        while (password.length() < minLength) {
            System.out.printf("Password must be at least %d characters long. %s: ", minLength, prompt);
            password = scanner.nextLine();
        }
        return password;
    }

    /**
     * Display a menu and read user choice
     */
    public static int readMenuChoice(String title, String[] options) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(title);
        System.out.println("=".repeat(50));
        
        for (int i = 0; i < options.length; i++) {
            System.out.printf("%d. %s%n", i + 1, options[i]);
        }
        System.out.println("0. Exit");
        
        return readInt("Enter your choice", 0, options.length);
    }

    /**
     * Wait for user to press Enter
     */
    public static void waitForEnter() {
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clear console (simulate)
     */
    public static void clearConsole() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    /**
     * Display a formatted header
     */
    public static void displayHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(centerText(title, 60));
        System.out.println("=".repeat(60));
    }

    /**
     * Display a formatted separator
     */
    public static void displaySeparator() {
        System.out.println("-".repeat(60));
    }

    /**
     * Center text within given width
     */
    public static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }

    /**
     * Display error message in formatted way
     */
    public static void displayError(String message) {
        System.out.println("\n❌ ERROR: " + message);
        System.out.println();
    }

    /**
     * Display success message in formatted way
     */
    public static void displaySuccess(String message) {
        System.out.println("\n✅ SUCCESS: " + message);
        System.out.println();
    }

    /**
     * Display info message in formatted way
     */
    public static void displayInfo(String message) {
        System.out.println("\nℹ️ INFO: " + message);
        System.out.println();
    }

    /**
     * Display warning message in formatted way
     */
    public static void displayWarning(String message) {
        System.out.println("\n⚠️ WARNING: " + message);
        System.out.println();
    }

    /**
     * Format text in a table-like structure
     */
    public static void displayTable(String[] headers, String[][] data) {
        if (headers.length == 0 || data.length == 0) {
            System.out.println("No data to display.");
            return;
        }

        // Calculate column widths
        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
            for (String[] row : data) {
                if (i < row.length && row[i] != null) {
                    columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                }
            }
        }

        // Print headers
        displaySeparator();
        for (int i = 0; i < headers.length; i++) {
            System.out.printf("| %-" + columnWidths[i] + "s ", headers[i]);
        }
        System.out.println("|");
        displaySeparator();

        // Print data
        for (String[] row : data) {
            for (int i = 0; i < headers.length; i++) {
                String value = (i < row.length && row[i] != null) ? row[i] : "";
                System.out.printf("| %-" + columnWidths[i] + "s ", value);
            }
            System.out.println("|");
        }
        displaySeparator();
    }

    /**
     * Validate email format
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Close scanner (call this when application exits)
     */
    public static void closeScanner() {
        scanner.close();
    }
}
