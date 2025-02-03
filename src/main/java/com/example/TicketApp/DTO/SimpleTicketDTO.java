package com.example.TicketApp.DTO;


import lombok.Data;

import java.time.LocalDateTime;


@Data
public class SimpleTicketDTO {
    private long ticketId;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public SimpleTicketDTO(long ticketId, String description, String status, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

}
