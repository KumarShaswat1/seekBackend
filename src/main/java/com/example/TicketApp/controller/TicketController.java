package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.services.TicketResponseService;
import com.example.TicketApp.services.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/ticket")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketResponseService ticketResponseService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTickets(
            @RequestParam long userId,
            @RequestParam String role,
            @RequestParam String status,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page, // Default to the first page
            @RequestParam(defaultValue = "10") int size // Default to 10 items per page
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Fetch paginated tickets
            Page<SimpleTicketDTO> paginatedTickets = ticketService.getFilteredTickets(userId, role, status, category, page, size);
            response.put("status", "success");
            response.put("data", Collections.singletonMap("tickets", paginatedTickets.getContent()));
            response.put("totalElements", paginatedTickets.getTotalElements());
            response.put("totalPages", paginatedTickets.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search/{userId}/{ticketId}")
    public ResponseEntity<Map<String, Object>> searchTicket(
            @PathVariable long userId,
            @PathVariable long ticketId,
            @RequestParam(defaultValue = "0") int page, // Default to the first page
            @RequestParam(defaultValue = "10") int size // Default to 10 items per page
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Fetch the TicketDTO using the service method
            TicketDTO ticketDTO = ticketService.searchTicket(userId, ticketId, page, size);
            response.put("status", "success");
            response.put("data", ticketDTO);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/count/search")
    public ResponseEntity<Map<String, Object>> getTicketCount(@RequestParam long userId,
                                                              @RequestParam String role,
                                                              @RequestParam String category) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Long> count = ticketService.getCountActiveResolved(userId, role, category);
            response.put("status", "success");
            response.put("data", count);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            // Handle unexpected server errors
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @GetMapping("/{ticket-id}/response")
    public ResponseEntity<?> getAllTicketResponses(@PathVariable("ticket-id") long ticketId,
                                                   @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Call the service to get all replies for the given ticketId
            List<TicketResponseDTO> replies = ticketService.getAllTicketResponses(userId, ticketId);
            if (replies.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Ticket not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
            }
            // Return success response with replies
            response.put("status", "success");
            response.put("message", "Replies fetched successfully");
            response.put("data", Collections.singletonMap("replies", replies));
            return ResponseEntity.status(HttpStatus.OK).body(response);  // 200 OK
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
    private static final Logger logger = Logger.getLogger(TicketService.class.getName());

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestParam long userId,
                                          @RequestParam String category,
                                          @RequestParam(required = false) String description) {

        Map<String, Object> response = new HashMap<>();
        try {
            if (description == null || description.isEmpty()) {
                description = "No description provided by the user.";
            }

            Ticket createdTicket = ticketService.createTicket(userId, category, description);
            response.put("status", "success");
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", createdTicket.getTicketId());
            data.put("message", "Ticket created successfully");
            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.severe("Bad request: " + e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // 400 Bad Request
        } catch (Exception e) {
            logger.severe("Internal server error: " + e.getMessage());
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
}
