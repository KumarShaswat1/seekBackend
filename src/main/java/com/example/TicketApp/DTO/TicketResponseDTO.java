package com.example.TicketApp.DTO;

import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class TicketResponseDTO {
    private Long responseId;
    private Long ticketId;
    private String responseText;
    private String role;
    private String userEmail;
    private String agentEmail;  // Add agent's email
    private LocalDateTime responseTime;  // Add the missing response time field
}
