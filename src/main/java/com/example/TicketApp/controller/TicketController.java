package com.example.TicketApp.controller;

import com.example.TicketApp.CustomErrors.BookingNotFoundException;
import com.example.TicketApp.CustomErrors.UserNotAuthorizedException;
import com.example.TicketApp.CustomErrors.UserNotFoundException;
import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketRequestDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.constants.Constants;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.services.TicketResponseService;
import com.example.TicketApp.services.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/ticket")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final TicketService ticketService;
    private final TicketResponseService ticketResponseService;

    // Constructor Injection
    public TicketController(TicketService ticketService, TicketResponseService ticketResponseService) {
        this.ticketService = ticketService;
        this.ticketResponseService = ticketResponseService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTickets(
            @RequestParam long user_id,
            @RequestParam String role,
            @RequestParam(required = false, defaultValue = Constants.STATUS_ALL) String ticket_status) {

        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Searching tickets for userId: {}, role: {}, status: {}", user_id, role, ticket_status);
            Map<String, List<SimpleTicketDTO>> tickets = ticketService.getFilteredTickets(user_id, role, ticket_status);

            response.put("status", Constants.STATUS_SUCCESS);
            response.put("data", tickets);

            return ResponseEntity.ok(response);
        } catch (BookingNotFoundException e) {
            logger.error("Booking not found: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
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
            logger.info("Fetching ticket details for userId: {}, ticketId: {}, page: {}, size: {}", userId, ticketId, page, size);
            TicketDTO ticketDTO = ticketService.searchTicket(userId, ticketId, page, size);
            response.put("status", Constants.STATUS_SUCCESS);
            response.put("data", ticketDTO);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/count/search")
    public ResponseEntity<Map<String, Object>> getTicketCount(@RequestParam long userId,
                                                              @RequestParam String role,
                                                              @RequestParam String category) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Fetching ticket count for userId: {}, role: {}, category: {}", userId, role, category);
            Map<String, Long> count = ticketService.getCountActiveResolved(userId, role, category);
            response.put("status", Constants.STATUS_SUCCESS);
            response.put("data", count);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{ticket-id}/response")
    public ResponseEntity<?> getAllTicketResponses(@PathVariable("ticket-id") long ticketId,
                                                   @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Fetching all responses for ticketId: {} by userId: {}", ticketId, userId);
            List<TicketResponseDTO> replies = ticketService.getAllTicketResponses(userId, ticketId);
            if (replies.isEmpty()) {
                logger.warn("No responses found for ticketId: {}", ticketId);
                response.put("status", Constants.STATUS_ERROR);
                response.put("message", "Ticket not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
            }
            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", "Replies fetched successfully");
            response.put("data", Collections.singletonMap("replies", replies));
            return ResponseEntity.status(HttpStatus.OK).body(response);  // 200 OK
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody TicketRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Creating ticket for userId: {} with bookingId: {}", request.getUserId(), request.getBookingId());
            Ticket createdTicket = ticketService.createTicket(
                    request.getUserId(),
                    request.getBookingId(),
                    request.getDescription(),
                    request.getRole()
            );
            response.put("status", Constants.STATUS_SUCCESS);
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", createdTicket.getTicketId());
            data.put("message", "Ticket created successfully");
            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // 400 Bad Request
        } catch (BookingNotFoundException e) {
            logger.error("Booking not found: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", "Invalid booking id.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
        } catch (UserNotAuthorizedException e) {
            logger.error("User not authorized: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);  // 403 Forbidden
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
}

