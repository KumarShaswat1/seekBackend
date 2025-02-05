package com.example.TicketApp.CustomErrors;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String message, Object... args) {
        super(String.format(message, args));
    }
}
