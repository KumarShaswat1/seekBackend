package com.example.TicketApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SimpleTicketDTO {
    private Long ticketId;
    private String description;
    private String status;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String agentEmail; // Email of the agent (if assigned)
    private String userEmail;  // Email of the user (customer)
}