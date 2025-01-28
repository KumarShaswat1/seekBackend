package com.example.TicketApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSignRequestDTO {
    private String email;
    private String password;
    private String role;
}
