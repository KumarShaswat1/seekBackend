package com.example.TicketApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
public class TicketRequestDTO {

    @JsonProperty("user_id")
    private long userId;

    @JsonProperty("booking_id")
    private Long bookingId;
    private String description;
    private String role;
}
