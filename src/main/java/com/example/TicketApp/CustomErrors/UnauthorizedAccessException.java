package com.example.TicketApp.CustomErrors;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("Unauthorized access");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    // Constructor with custom message and cause
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause
    public UnauthorizedAccessException(Throwable cause) {
        super(cause);
    }
}
