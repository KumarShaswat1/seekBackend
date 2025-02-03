package com.example.TicketApp.constants;

public class Constants {

    // HTTP Status Codes
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String MESSAGE_INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String MESSAGE_USER_NOT_FOUND = "User not found";
    public static final String MESSAGE_TICKET_NOT_FOUND = "Ticket not found";
    public static final String MESSAGE_INVALID_USERNAME_OR_PASSWORD = "Invalid username or password";
    public static final String MESSAGE_USER_REGISTERED_SUCCESSFULLY = "User registered successfully";
    public static final String MESSAGE_LOGIN_SUCCESSFUL = "Login successful";
    public static final String MESSAGE_REPLY_CREATED = "Reply created successfully";
    public static final String MESSAGE_REPLY_UPDATED = "Reply updated successfully";
    public static final String MESSAGE_REPLY_DELETED = "Reply deleted successfully";
    public static final String MESSAGE_TICKET_CREATED = "Ticket created successfully";
    public static final String MESSAGE_USER_ALREADY_EXISTS = "User with this email already exists";
    public static final String MESSAGE_INVALID_ROLE = "Role must be 'CUSTOMER' or 'AGENT'";

    // HTTP Status Codes (Numeric)
    public static final int HTTP_STATUS_CREATED = 201;
    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_NOT_FOUND = 404;
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;
    public static final int HTTP_STATUS_FORBIDDEN = 403;

    // Roles
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_AGENT = "AGENT";

    // Ticket Status
    public static final String STATUS_ALL = "ALL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_PREBOOKING = "Prebooking";
    public static final String STATUS_POSTBOOKING = "Postbooking";

    // Cache Settings
    public static final String CACHE_KEY_PREFIX = "ticket_counts::";
    public static final long CACHE_TTL = 30; // 30 minutes

    // Other Constants
    public static final String NO_EMAIL = "No Email";
    public static final String UNKNOWN = "UNKNOWN";
}
