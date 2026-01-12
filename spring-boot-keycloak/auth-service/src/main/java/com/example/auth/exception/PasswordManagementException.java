package com.example.auth.exception;

/**
 * Exception for password management operations
 */
public class PasswordManagementException extends RuntimeException {
    
    private String errorCode;
    
    public PasswordManagementException(String message) {
        super(message);
        this.errorCode = "PASSWORD_MANAGEMENT_ERROR";
    }
    
    public PasswordManagementException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PASSWORD_MANAGEMENT_ERROR";
    }
    
    public PasswordManagementException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
