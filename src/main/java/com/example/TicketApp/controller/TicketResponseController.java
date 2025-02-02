package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.constants.Constants;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.services.TicketResponseService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/ticket-response")
public class TicketResponseController {

    private static final Logger logger = LoggerFactory.getLogger(TicketResponseController.class);
    private final TicketResponseService ticketResponseService;

    // Constructor Injection
    public TicketResponseController(TicketResponseService ticketResponseService) {
        this.ticketResponseService = ticketResponseService;
    }

    // Endpoint to create a new reply (ticket response)
    @PostMapping("/{ticket-id}")
    public ResponseEntity<Map<String, Object>> createTicketResponse(
            @PathVariable("ticket-id") long ticketId,
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Creating ticket response for ticketId: {}", ticketId);

            // Extract user_id, role, and replyData from the request body
            long userId = Long.parseLong(requestBody.get("user_id").toString());
            String role = requestBody.get("role").toString();
            Map<String, Object> replyData = (Map<String, Object>) requestBody.get("replyData");

            // Call the service to create the reply
            TicketResponseDTO createdReply = ticketResponseService.createTicketReply(ticketId, userId, role, replyData);

            // Prepare response
            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", Constants.MESSAGE_REPLY_CREATED);
            response.put("data", createdReply);  // Include the DTO in the response

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.error("Error creating ticket response: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update Reply for only agents
    @PutMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> updateResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId,
                                            @RequestBody Map<String, String> updateText) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Updating response for ticketId: {} and responseId: {}", ticketId, responseId);
            String updatedText = updateText.get("updatedText");
            if (updatedText == null || updatedText.trim().isEmpty()) {
                throw new IllegalArgumentException("Update text cannot be empty");
            }

            // Update ticket response
            TicketResponse updatedReply = ticketResponseService.updateTicketResponse(userId, ticketId, responseId, updatedText);

            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", Constants.MESSAGE_REPLY_UPDATED);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating response: {}", e.getMessage());
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

    // Delete a ticket response
    @DeleteMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> deleteResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Deleting response for ticketId: {} and responseId: {}", ticketId, responseId);

            // Call service to delete response
            ticketResponseService.deleteTicketResponse(userId, ticketId, responseId);

            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", Constants.MESSAGE_REPLY_DELETED);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting response: {}", e.getMessage());
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

    // Update the status of the ticket response (for agents)
    @PutMapping("/{ticket-id}/update-status")
    public ResponseEntity<?> updateTicketResponseStatus(@PathVariable("ticket-id") long ticketId,
                                                        @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Updating ticket response status for ticketId: {}", ticketId);

            boolean statusUpdated = ticketResponseService.updateTicketResponseStatus(userId, ticketId);
            if (statusUpdated) {
                response.put("status", Constants.STATUS_SUCCESS);
                response.put("message", "Status changed successfully");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("status", Constants.STATUS_ERROR);
                response.put("message", Constants.MESSAGE_TICKET_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
            }
        } catch (IllegalArgumentException e) {
            logger.error("Error updating ticket response status: {}", e.getMessage());
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
