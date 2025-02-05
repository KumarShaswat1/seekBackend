package com.example.TicketApp.services;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.enums.Status;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.CustomErrors.BookingNotFoundException;
import com.example.TicketApp.CustomErrors.UserNotAuthorizedException;
import com.example.TicketApp.CustomErrors.UserNotFoundException;
import com.example.TicketApp.CustomErrors.UnauthorizedAccessException;
import com.example.TicketApp.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TicketResponseService {

    private static final Logger logger = LoggerFactory.getLogger(TicketResponseService.class);

    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final UserRespository userRespository;

    // Constructor Injection
    public TicketResponseService(TicketRepository ticketRepository, TicketResponseRepository ticketResponseRepository, UserRespository userRespository) {
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.userRespository = userRespository;
    }

    public TicketResponseDTO createTicketReply(long ticketId, long userId, String role, Map<String, Object> replyData) throws UnauthorizedAccessException {
        // Validate role
        validateRole(role);

        // Ensure reply data contains valid 'responseText'
        if (replyData == null || !replyData.containsKey("responseText") || replyData.get("responseText") == null) {
            logger.error(Constants.LOG_REPLY_DATA_INVALID);
            throw new IllegalArgumentException(Constants.MESSAGE_INVALID_USERNAME_OR_PASSWORD);
        }

        // Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException(Constants.LOG_TICKET_NOT_FOUND, ticketId));

        // Find the user
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Constants.LOG_USER_NOT_FOUND, userId));

        // Validate user's authorization for the role
        validateAuthorization(ticket, user, role);

        // Check if the role matches the allowed roles for the user in the context of the ticket
        if (!isValidRoleForTicket(ticket, user, role)) {
            logger.error(Constants.LOG_ROLE_NOT_AUTHORIZED, userId, role);
            throw new UnauthorizedAccessException(Constants.MESSAGE_INVALID_ROLE);
        }

        // Create and save the TicketResponse entity
        TicketResponse ticketResponse = new TicketResponse();
        ticketResponse.setTicket(ticket);
        ticketResponse.setUser(user);
        ticketResponse.setRole(Role.valueOf(role.toUpperCase())); // Ensure role is valid
        ticketResponse.setResponseText(replyData.get("responseText").toString());

        // Save the response to the repository
        TicketResponse savedResponse = ticketResponseRepository.save(ticketResponse);

        // Add the response to the ticket and save the ticket
        ticket.getResponses().add(savedResponse);
        ticketRepository.save(ticket);

        // Determine the reply user’s email
        String userEmail = user.getEmail();
        String agentEmail;

        if (role.equals(Constants.ROLE_AGENT)) {
            // Ensure ticket.getCustomer() is not null
            User customer = ticket.getCustomer();
            if (customer == null) {
                logger.error(Constants.LOG_CUSTOMER_NOT_FOUND);
                throw new IllegalStateException(Constants.MESSAGE_USER_NOT_FOUND);
            }
            agentEmail = customer.getEmail();
        } else {
            // Ensure ticket.getAgent() is not null
            User agent = ticket.getAgent();
            if (agent == null) {
                logger.error(Constants.LOG_AGENT_NOT_FOUND);
                throw new IllegalStateException(Constants.MESSAGE_USER_NOT_FOUND);
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

    // New helper method to validate the role based on the ticket context and user
    private boolean isValidRoleForTicket(Ticket ticket, User user, String role) {
        // Example validation logic for role
        if (role.equalsIgnoreCase(Constants.ROLE_AGENT)) {
            return user.equals(ticket.getAgent()); // Only the assigned agent can reply as AGENT
        } else if (role.equalsIgnoreCase(Constants.ROLE_CUSTOMER)) {
            return user.equals(ticket.getCustomer()); // Only the assigned customer can reply as CUSTOMER
        }
        return false; // Invalid role
    }

    // Update a ticket response
    public TicketResponse updateTicketResponse(long userId, long ticketId, long responseId, String updateText) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Constants.LOG_USER_NOT_FOUND, userId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException(Constants.LOG_TICKET_NOT_FOUND, ticketId));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException(Constants.LOG_INVALID_TICKET));

        if (!ticketResponse.getUser().equals(user)) {
            logger.error(Constants.LOG_USER_NOT_AUTHORIZED_UPDATE);
            throw new UserNotAuthorizedException(Constants.MESSAGE_USER_NOT_FOUND);
        }

        ticketResponse.setResponseText(updateText);
        return ticketResponseRepository.save(ticketResponse);
    }

    // Delete a ticket response
    public void deleteTicketResponse(long userId, long ticketId, long responseId) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Constants.LOG_USER_NOT_FOUND, userId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException(Constants.LOG_TICKET_NOT_FOUND, ticketId));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException(Constants.LOG_INVALID_TICKET));

        if (!ticketResponse.getUser().equals(user)) {
            logger.error(Constants.LOG_USER_NOT_AUTHORIZED_DELETE);
            throw new UserNotAuthorizedException(Constants.MESSAGE_USER_NOT_FOUND);
        }

        ticketResponseRepository.delete(ticketResponse);
    }

    // Update ticket status
    public boolean updateTicketResponseStatus(long userId, long ticketId) {
        // Find the user by ID
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Constants.LOG_USER_NOT_FOUND, userId));

        // Ensure only agents can update the status
        if (user.getRole() != Role.AGENT) {
            logger.error(Constants.LOG_ACCESS_DENIED);
            throw new UserNotAuthorizedException(Constants.MESSAGE_INVALID_ROLE);
        }

        // Find the ticket by ID
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException(Constants.LOG_TICKET_NOT_FOUND, ticketId));

        // Ensure the user is the assigned agent for this ticket
        if (ticket.getAgent() == null || !ticket.getAgent().equals(user)) {
            logger.error(Constants.LOG_USER_NOT_AUTHORIZED_STATUS_UPDATE);
            throw new UserNotAuthorizedException(Constants.MESSAGE_USER_NOT_FOUND);
        }

        // Update ticket status to RESOLVED
        ticket.setStatus(Status.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());

        // Save the updated ticket to the database
        ticketRepository.save(ticket);

        return true;
    }

    // Helper method: Validate role
    private void validateRole(String role) {
        if (role == null || (!role.equalsIgnoreCase(Constants.ROLE_AGENT) && !role.equalsIgnoreCase(Constants.ROLE_CUSTOMER))) {
            logger.error(Constants.LOG_ROLE_VALIDATION_FAILED);
            throw new IllegalArgumentException(Constants.MESSAGE_INVALID_ROLE);
        }
    }

    // Helper method: Validate authorization
    private void validateAuthorization(Ticket ticket, User user, String role) {
        if (Constants.ROLE_AGENT.equalsIgnoreCase(role)) {
            if (ticket.getAgent() == null || !ticket.getAgent().equals(user)) {
                logger.error(Constants.LOG_USER_NOT_AUTHORIZED, role);
                throw new UserNotAuthorizedException(Constants.MESSAGE_USER_NOT_FOUND);
            }
        } else if (Constants.ROLE_CUSTOMER.equalsIgnoreCase(role)) {
            if (ticket.getCustomer() == null || !ticket.getCustomer().equals(user)) {
                logger.error(Constants.LOG_USER_NOT_AUTHORIZED, role);
                throw new UserNotAuthorizedException(Constants.MESSAGE_USER_NOT_FOUND);
            }
        }
    }
}
