package com.example.TicketApp.services;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;



@Service
public class TicketResponseService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketResponseRepository ticketResponseRepository;

    @Autowired
    private UserRespository userRespository;
    public TicketResponseDTO createTicketReply(long ticketId, long userId, String role, Map<String, Object> replyData) {
        // Validate role
        validateRole(role);

        // Ensure reply data contains valid 'responseText'
        if (replyData == null || !replyData.containsKey("responseText") || replyData.get("responseText") == null) {
            throw new IllegalArgumentException("Reply data must include a non-null 'responseText' field.");
        }

        // Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));

        // Find the user
        User user = userRespository.findById(userId) // Fixed typo
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Validate user's authorization for the role
        validateAuthorization(ticket, user, role);

        // Create and save the TicketResponse entity
        TicketResponse ticketResponse = new TicketResponse();
        ticketResponse.setTicket(ticket);
        ticketResponse.setUser(user);
        ticketResponse.setRole(TicketResponse.Role.valueOf(role.toUpperCase())); // Ensure role is valid
        ticketResponse.setResponseText(replyData.get("responseText").toString());

        // Save the response to the repository
        TicketResponse savedResponse = ticketResponseRepository.save(ticketResponse);

        // Add the response to the ticket and save the ticket
        ticket.getResponses().add(savedResponse);
        ticketRepository.save(ticket);

        // Determine the reply user’s email
        String userEmail = user.getEmail();
        String agentEmail;

        if (role.equals("AGENT")) {
            // Ensure ticket.getCustomer() is not null
            User customer = ticket.getCustomer();
            if (customer == null) {
                throw new IllegalStateException("Customer not found for the ticket.");
            }
            agentEmail = customer.getEmail();
        } else {
            // Ensure ticket.getAgent() is not null
            User agent = ticket.getAgent();
            if (agent == null) {
                throw new IllegalStateException("Agent not found for the ticket.");
            }
            agentEmail = agent.getEmail();
        }

        // Map the saved TicketResponse to a TicketResponseDTO and return it
        return new TicketResponseDTO(
                savedResponse.getResponseId(),
                ticket.getTicketId(),
                savedResponse.getResponseText(),
                savedResponse.getRole().toString(),
                userEmail,
                agentEmail,
                savedResponse.getCreatedAt() // Response time
        );
    }

    // Update a ticket response
    public TicketResponse updateTicketResponse(long userId, long ticketId, long responseId, String updateText) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!ticketResponse.getUser().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to update this reply");
        }

        ticketResponse.setResponseText(updateText);
        return ticketResponseRepository.save(ticketResponse);
    }

    // Delete a ticket response
    public void deleteTicketResponse(long userId, long ticketId, long responseId) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found"));

        if (!ticketResponse.getUser().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to delete this reply");
        }

        ticketResponseRepository.delete(ticketResponse);
    }

    // Update ticket status
    public boolean updateTicketResponseStatus(long userId, long ticketId) {
        // Find the user by ID
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Ensure only agents can update the status
        if (user.getRole() != User.Role.AGENT) {
            throw new IllegalArgumentException("Access denied. Only agents can update the status.");
        }

        // Find the ticket by ID
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        // Ensure the user is the assigned agent for this ticket
        if (ticket.getAgent() == null || !ticket.getAgent().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to update the status of this ticket.");
        }

        // Update ticket status to RESOLVED
        ticket.setStatus(Ticket.Status.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());

        // Save the updated ticket to the database
        ticketRepository.save(ticket);

        return true;
    }


    // Helper method: Validate role
    private void validateRole(String role) {
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }
    }

    // Helper method: Validate authorization
    private void validateAuthorization(Ticket ticket, User user, String role) {
        if ("AGENT".equalsIgnoreCase(role)) {
            if (ticket.getAgent() == null || !ticket.getAgent().equals(user)) {
                throw new IllegalArgumentException("User is not authorized to reply as an agent for this ticket.");
            }
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            if (ticket.getCustomer() == null || !ticket.getCustomer().equals(user)) {
                throw new IllegalArgumentException("User is not authorized to reply as a customer for this ticket.");
            }
        }
    }
}
