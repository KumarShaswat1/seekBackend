package com.example.TicketApp.DTO;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimpleTicketDTO {
    private long ticketId;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private String customerEmail;
    private String agentEmail;

    // Constructor
    public SimpleTicketDTO(long ticketId, String description, String status, LocalDateTime createdAt, String customerEmail, String agentEmail) {
        this.ticketId = ticketId;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.customerEmail = customerEmail;
        this.agentEmail = agentEmail;
    }
}
