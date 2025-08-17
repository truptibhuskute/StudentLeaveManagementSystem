package com.studentleave.exceptions;

/**
 * Base exception class for the Leave Management System
 * Demonstrates custom exception handling and error management
 */
public class LeaveManagementException extends Exception {
    private final String errorCode;
    private final ErrorType errorType;

    public enum ErrorType {
        VALIDATION_ERROR,
        DATABASE_ERROR,
        AUTHENTICATION_ERROR,
        AUTHORIZATION_ERROR,
        BUSINESS_LOGIC_ERROR,
        SYSTEM_ERROR
    }

    public LeaveManagementException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
        this.errorType = ErrorType.SYSTEM_ERROR;
    }

    public LeaveManagementException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
        this.errorType = ErrorType.SYSTEM_ERROR;
    }

    public LeaveManagementException(String message, String errorCode, ErrorType errorType) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public LeaveManagementException(String message, Throwable cause, String errorCode, ErrorType errorType) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", errorCode, errorType, getMessage());
    }
}

/**
 * Exception thrown when database operations fail
 */
class DatabaseException extends LeaveManagementException {
    public DatabaseException(String message) {
        super(message, "DB_ERROR", ErrorType.DATABASE_ERROR);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause, "DB_ERROR", ErrorType.DATABASE_ERROR);
    }
}

/**
 * Exception thrown when authentication fails
 */
class AuthenticationException extends LeaveManagementException {
    public AuthenticationException(String message) {
        super(message, "AUTH_ERROR", ErrorType.AUTHENTICATION_ERROR);
    }
}

/**
 * Exception thrown when user lacks required permissions
 */
class AuthorizationException extends LeaveManagementException {
    public AuthorizationException(String message) {
        super(message, "AUTHZ_ERROR", ErrorType.AUTHORIZATION_ERROR);
    }
}

/**
 * Exception thrown when business rules are violated
 */
class BusinessLogicException extends LeaveManagementException {
    public BusinessLogicException(String message) {
        super(message, "BUSINESS_ERROR", ErrorType.BUSINESS_LOGIC_ERROR);
    }
}

/**
 * Exception thrown when validation fails
 */
class ValidationException extends LeaveManagementException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", ErrorType.VALIDATION_ERROR);
    }
}
