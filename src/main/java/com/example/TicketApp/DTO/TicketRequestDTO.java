package com.example.TicketApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;


public class TicketRequestDTO {
    private long user_id;
    private String booking_id;
    private String description;
    private String role;
    public long getUserId() { return user_id; }
    public String getBookingId() { return booking_id; }
    public String getDescription() { return description; }
    public String getRole() { return role; }
}