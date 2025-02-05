package com.example.TicketApp.services;

import com.example.TicketApp.CustomErrors.BookingNotFoundException;
import com.example.TicketApp.CustomErrors.UserNotAuthorizedException;
import com.example.TicketApp.CustomErrors.UserNotFoundException;
import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.constants.Constants;
import com.example.TicketApp.entity.Booking;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Category;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.enums.Status;
import com.example.TicketApp.repository.BookingRespository;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final UserRespository userRespository;
    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final BookingRespository bookingRespository;
    private final RedisTemplate<String, Object> redisTemplate;


    public TicketService(UserRespository userRespository, TicketRepository ticketRepository, TicketResponseRepository ticketResponseRepository,
                         BookingRespository bookingRespository, RedisTemplate<String, Object> redisTemplate) {
        this.userRespository = userRespository;
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.bookingRespository = bookingRespository;
        this.redisTemplate = redisTemplate;
    }


    public Map<String, List<SimpleTicketDTO>> getFilteredTickets(long userId, String role, String status, Pageable pageable) {
        // Validate user existence
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Validate role
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }

        List<Ticket> tickets;

        // Determine tickets based on role
        if ("AGENT".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsAgent();
        } else {
            tickets = user.getTicketsAsCustomer();
        }

        // Filter tickets based on status (ACTIVE, RESOLVED or ALL)
        if (status != null && !status.equalsIgnoreCase("ALL") && !status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("RESOLVED")) {
            throw new IllegalArgumentException("Invalid status. Status must be 'ACTIVE', 'RESOLVED', or 'ALL'.");
        }

        tickets = tickets.stream()
                .filter(ticket -> "ALL".equalsIgnoreCase(status) || ticket.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        // Separate tickets into Prebooking and Postbooking
        List<SimpleTicketDTO> prebookingTickets = new ArrayList<>();
        List<SimpleTicketDTO> postbookingTickets = new ArrayList<>();

        for (Ticket ticket : tickets) {
            // Get Customer and Agent emails
            String customerEmail = ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : "No Email";
            String agentEmail = ticket.getAgent() != null ? ticket.getAgent().getEmail() : "No Email";

            // Create DTO with email addresses included
            SimpleTicketDTO dto = new SimpleTicketDTO(
                    ticket.getTicketId(),
                    ticket.getDescription(),
                    ticket.getStatus().name(),
                    ticket.getCreatedAt(),
                    customerEmail,
                    agentEmail
            );

            if (ticket.getBooking() == null) {
                // No booking ID exists, this is a prebooking ticket
                prebookingTickets.add(dto);
            } else {
                postbookingTickets.add(dto);
            }
        }

        // Apply pagination to Prebooking and Postbooking Tickets
        List<SimpleTicketDTO> paginatedPrebookingTickets = paginate(prebookingTickets, pageable);
        List<SimpleTicketDTO> paginatedPostbookingTickets = paginate(postbookingTickets, pageable);

        // Create a map to hold prebooking and postbooking tickets
        Map<String, List<SimpleTicketDTO>> result = new HashMap<>();
        result.put("PrebookingTickets", paginatedPrebookingTickets);
        result.put("PostbookingTickets", paginatedPostbookingTickets);

        return result;
    }

    // Helper method for pagination
    private List<SimpleTicketDTO> paginate(List<SimpleTicketDTO> tickets, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), tickets.size());

        // Ensure no negative indices
        if (start >= tickets.size()) {
            return Collections.emptyList();  // Return empty if offset exceeds ticket size
        }

        // Handle cases where subList could go out of bounds
        if (end > tickets.size()) {
            end = tickets.size();
        }

        return tickets.subList(start, end);
    }


    // Helper method to validate postbooking status
    private boolean isPostBookingValid(Ticket ticket, long userId) {
        if (ticket.getBooking() != null && ticket.getBooking().getUser() != null) {
            Booking booking = ticket.getBooking();
            return (booking.getUser().getUserId() == userId);
        }
        return false;
    }


    public Map<String, Long> getCountActiveResolved(long userId, String role, String category) {
        // Validate role
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }

        List<Ticket> tickets = ticketRepository.findAll();
        // Generate cache key (including category)
        String cacheKey = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category + "::" + tickets.size();

        // Try to get cached result
        Map<String, Long> cachedResult = (Map<String, Long>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Cache miss - compute fresh result
        tickets=tickets.stream()
                .filter(ticket -> {
                    // Filter by userId and role
                    boolean isUserMatch = (role.equalsIgnoreCase("AGENT") && ticket.getAgent() != null && ticket.getAgent().getUserId() == userId) ||
                            (role.equalsIgnoreCase("CUSTOMER") && ticket.getCustomer() != null && ticket.getCustomer().getUserId() == userId);

                    // Filter by category
                    boolean isCategoryMatch = category != null  &&  category.equalsIgnoreCase("ALL") || category.equalsIgnoreCase(String.valueOf(ticket.getCategory()));

                    return isUserMatch && isCategoryMatch;
                })
                .collect(Collectors.toList());

        long activeCount = tickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.ACTIVE)
                .count();

        long resolvedCount = tickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.RESOLVED)
                .count();

        Map<String, Long> counts = new HashMap<>();
        counts.put("active", activeCount);
        counts.put("resolved", resolvedCount);

        // Cache the result with TTL
        redisTemplate.opsForValue().set(cacheKey, counts, Duration.ofMinutes(Constants.CACHE_TTL));

        return counts;
    }



    public Map<String, Object> searchTicket(long userId, long ticketId, int page, int size) {
        Map<String, Object> responseMap = new HashMap<>();

        // Validate and retrieve the user
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Validate and retrieve the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException("Ticket not found with ID: " + ticketId));

        // Validate ticket ownership or association (Customer or Agent)
        if (!ticket.getCustomer().equals(user) && (ticket.getAgent() == null || !ticket.getAgent().equals(user))) {
            throw new UserNotAuthorizedException("User ID " + userId + " is not authorized to view ticket ID " + ticketId);
        }

        // Map the ticket fields into the response
        Map<String, Object> ticketDetails = new HashMap<>();
        ticketDetails.put("ticketId", ticket.getTicketId());
        ticketDetails.put("status", ticket.getStatus());
        ticketDetails.put("category", ticket.getCategory());
        ticketDetails.put("time", ticket.getCreatedAt());  // Assuming 'createdAt' is the 'time' you're referring to
        ticketDetails.put("description", ticket.getDescription());  // Assuming 'description' is a field in the Ticket

        // Split ticket responses into prebooking and postbooking (merged later)
        List<TicketResponse> mergedResponses = new ArrayList<>();

        if (ticket.getResponses() != null) {
            for (TicketResponse response : ticket.getResponses()) {
                // If there is no booking, consider it a prebooking response, otherwise it's a postbooking response
                if (ticket.getBooking() == null) {
                    mergedResponses.add(response); // prebooking response
                } else {
                    mergedResponses.add(response); // postbooking response
                }
            }
        } else {
            logger.warn("Ticket ID {} has no responses", ticketId);
        }

        // Paginate the merged responses
        List<TicketResponseDTO> mergedDTOs = paginateAndMapResponses(mergedResponses, page, size, ticket, user);

        // Add ticket details and responses to the response map
        ticketDetails.put("responses", mergedDTOs);

        return ticketDetails;
    }


    private List<TicketResponseDTO> paginateAndMapResponses(List<TicketResponse> responses, int page, int size, Ticket ticket, User user) {
        // Handle pagination logic
        if (page < 0 || size <= 0 || page * size >= responses.size()) {
            return Collections.emptyList(); // Return an empty list if the page/size is invalid
        }

        int start = page * size;
        int end = Math.min(start + size, responses.size());
        List<TicketResponse> paginatedResponses = responses.subList(start, end);

        // Map the paginated responses to DTOs
        List<TicketResponseDTO> responseDTOs = new ArrayList<>();
        for (TicketResponse response : paginatedResponses) {
            String userEmail;
            String agentEmail;

            // For CUSTOMER: userEmail is the customer's email, and agentEmail is the agent's email
            // For AGENT: userEmail is the agent's email, and agentEmail is the customer's email
            if (user.getRole() == null || user.getRole() == Role.CUSTOMER) {
                userEmail = ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : "No Email";
                agentEmail = ticket.getAgent() != null ? ticket.getAgent().getEmail() : "No Email";
            } else if (user.getRole() == Role.AGENT) {
                userEmail = ticket.getAgent() != null ? ticket.getAgent().getEmail() : "No Email";
                agentEmail = ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : "No Email";
            } else {
                // Default case if the role is null or unrecognized
                userEmail = "No Email";
                agentEmail = "No Email";
            }

            // Add the response to DTO list
            responseDTOs.add(new TicketResponseDTO(
                    response.getResponseId(),
                    ticket.getTicketId(),
                    response.getResponseText(),
                    response.getRole() != null ? response.getRole().toString() : "UNKNOWN",
                    userEmail,        // from
                    agentEmail,       // to
                    response.getCreatedAt()
            ));
        }

        return responseDTOs;
    }



    public List<TicketResponseDTO> getAllTicketResponses(long userId, long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException("Ticket not found with ID: " + ticketId));

        List<TicketResponse> ticketResponses = ticket.getResponses();
        List<TicketResponseDTO> repliesDTO = new ArrayList<>();
        for (TicketResponse ticketResponse : ticketResponses) {
            User user = ticketResponse.getUser();

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    ticketResponse.getResponseId(),                // Response ID
                    ticket.getTicketId(),                          // Associated Ticket ID
                    ticketResponse.getResponseText(),              // Response Text
                    ticketResponse.getRole().toString(),           // Role
                    ticketResponse.getUser().getEmail(),           // User's Email
                    ticket.getAgent() != null ? ticket.getAgent().getEmail() : null, // Agent's Email
                    ticketResponse.getCreatedAt()                  // Created At Timestamp
            );

            repliesDTO.add(responseDTO);
        }

        return repliesDTO;
    }

    public Ticket createTicket(long userId, Long bookingId, String description, String role) {
        logger.info("Creating ticket for userId: " + userId + " with bookingId: " + bookingId);

        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (!role.equalsIgnoreCase("CUSTOMER")) {
            throw new UserNotAuthorizedException("Only customers can create tickets.");
        }

        Ticket ticket = new Ticket();
        ticket.setCustomer(user);
        ticket.setDescription(description);
        ticket.setStatus(Status.ACTIVE);
        ticket.setCreatedAt(LocalDateTime.now());

        if (bookingId == null) {
            // Prebooking Ticket (No Booking ID)
            ticket.setCategory(Category.PREBOOKING);
            ticket.setBooking(null);
        } else {
            // Postbooking Ticket (Booking ID Provided)
            Booking booking = bookingRespository.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException("Invalid booking id: " + bookingId));

            if (!booking.getUser().getUserId().equals(user.getUserId())) {
                throw new UserNotAuthorizedException("User is not authorized to access this booking.");
            }

            ticket.setCategory(Category.POSTBOOKING);
            ticket.setBooking(booking);
        }

        User agent = assignAgentToTicket();
        ticket.setAgent(agent);

        Ticket savedTicket = ticketRepository.save(ticket);
        logger.info("Ticket created successfully with ID: " + savedTicket.getTicketId());

        return savedTicket;
    }

    private User assignAgentToTicket() {
        List<User> agents = userRespository.findByRole(Role.AGENT);
        if (agents.isEmpty()) {
            throw new IllegalStateException("No available agents for ticket assignment");
        }
        return agents.get(new Random().nextInt(agents.size()));
    }
}
