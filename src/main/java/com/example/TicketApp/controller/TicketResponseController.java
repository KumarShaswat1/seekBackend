package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.services.TicketResponseService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/ticket-response")
public class TicketResponseController {

    @Autowired
    private TicketResponseService ticketResponseService;

    // Endpoint to create a new reply (ticket response)
    @PostMapping("/{ticket-id}")
    public ResponseEntity<Map<String, Object>> createTicketResponse(
            @PathVariable("ticket-id") long ticketId,
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();
        try {
            // Extract user_id, role, and replyData from the request body
            long userId = Long.parseLong(requestBody.get("user_id").toString());
            String role = requestBody.get("role").toString();
            Map<String, Object> replyData = (Map<String, Object>) requestBody.get("replyData");

            // Call the service to create the reply
            TicketResponseDTO createdReply = ticketResponseService.createTicketReply(ticketId, userId, role, replyData);

            // Prepare response
            response.put("status", "success");
            response.put("message", "Reply created successfully");
            response.put("data", createdReply);  // Include the DTO in the response

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    //    Update Reply for only agents
    @PutMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> updateResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId,
                                            @RequestBody Map<String, String> updateText) {
        Map<String, Object> response = new HashMap<>();
        try {
            String updatedText = updateText.get("updatedText");
            if (updatedText == null || updatedText.trim().isEmpty()) {
                throw new IllegalArgumentException("Update text cannot be empty");
            }
            TicketResponse updatedReply = ticketResponseService.updateTicketResponse(userId, ticketId, responseId, updatedText);

            response.put("status", "success");
            response.put("message", "Reply updated successfully");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);  // 403 Forbidden
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }


    @DeleteMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> deleteResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ticketResponseService.deleteTicketResponse(userId, ticketId, responseId);
            response.put("status", "success");
            response.put("message", "Reply deleted successfully");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);  // 403 Forbidden

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }

    @PutMapping("/{ticket-id}/update-status")
    public ResponseEntity<?> updateTicketResponseStatus(@PathVariable("ticket-id") long ticketId,
                                                        @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean statusUpdated = ticketResponseService.updateTicketResponseStatus(userId, ticketId);
            if (statusUpdated) {
                response.put("status", "success");
                response.put("message", "Status changed successfully");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("status", "error");
                response.put("message", "Ticket not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
            }
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);  // 403 Forbidden
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
}
