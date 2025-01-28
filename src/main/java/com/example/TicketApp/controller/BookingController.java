package com.example.TicketApp.controller;

import com.example.TicketApp.services.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/{booking-id}/validate")
    public ResponseEntity<?> validateBooking(@RequestParam long userId, @PathVariable("booking-id") long bookingId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = bookingService.validateBooking(userId, bookingId);
            if (isValid) {
                response.put("status", "success");
                response.put("message", "true");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("status", "error");
                response.put("message", "Booking not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
}
