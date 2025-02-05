package com.example.TicketApp.CustomErrors;


public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }

    public BookingNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
