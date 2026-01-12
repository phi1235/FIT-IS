package com.example.auth.exception;

/**
 * Exception for resource not found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private String errorCode;
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }
    
    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
