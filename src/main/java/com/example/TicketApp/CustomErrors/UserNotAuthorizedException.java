package com.example.TicketApp.CustomErrors;

public class UserNotAuthorizedException extends RuntimeException {

    public UserNotAuthorizedException(String message) {
        super(message);
    }

    public UserNotAuthorizedException(String message, Object... args) {
        super(String.format(message, args));
    }
}
