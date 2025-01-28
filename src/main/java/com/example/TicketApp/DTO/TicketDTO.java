package com.example.TicketApp.DTO;

import com.example.TicketApp.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TicketDTO {
    private Long ticketId;
    private String description;
    private String status;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketResponseDTO> responses;  // List of associated responses
}
