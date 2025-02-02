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
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    public static long count=0;
    private final UserRespository userRespository;
    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final BookingRespository bookingRespository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Constructor Injection
    public TicketService(UserRespository userRespository, TicketRepository ticketRepository, TicketResponseRepository ticketResponseRepository,
                         BookingRespository bookingRespository, RedisTemplate<String, Object> redisTemplate) {
        this.userRespository = userRespository;
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.bookingRespository = bookingRespository;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, List<SimpleTicketDTO>> getFilteredTickets(long userId, String role, String status) {
        // Validate user existence
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        List<Ticket> tickets;

        // Determine tickets based on role
        if ("AGENT".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsAgent();
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsCustomer();
        } else {
            throw new IllegalArgumentException("Invalid role. Must be 'AGENT' or 'CUSTOMER'.");
        }

        // Separate tickets into Prebooking and Postbooking
        List<SimpleTicketDTO> prebookingTickets = new ArrayList<>();
        List<SimpleTicketDTO> postbookingTickets = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (filterTickets(ticket, status)) {
                SimpleTicketDTO dto = new SimpleTicketDTO(
                        ticket.getTicketId(),
                        ticket.getDescription(),
                        ticket.getStatus().name(),
                        ticket.getCreatedAt()
                );

                if (isPreBooking(ticket)) {
                    prebookingTickets.add(dto);
                } else {
                    postbookingTickets.add(dto);
                }
            }
        }

        // Response structure
        Map<String, List<SimpleTicketDTO>> responseData = new HashMap<>();
        responseData.put("Prebooking", prebookingTickets);
        responseData.put("Postbooking", postbookingTickets);

        return responseData;
    }

    // Helper method to filter tickets based on status
    private boolean filterTickets(Ticket ticket, String status) {
        return "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(ticket.getStatus().name());
    }

    // Determine if a ticket is prebooking (No booking ID → Prebooking, Booking exists → Postbooking)
    private boolean isPreBooking(Ticket ticket) {
        return ticket.getBooking() == null;
    }

    public Map<String, Long> getCountActiveResolved(long userId, String role, String category) {
        // Validate role
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }

        // Generate cache key
        String cacheKey = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category;

        // Try to get cached result
        Map<String, Long> cachedResult = (Map<String, Long>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Cache miss - compute fresh result
        List<Ticket> tickets = ticketRepository.findAll().stream()
                .filter(ticket -> {
                    if (role.equalsIgnoreCase("AGENT")) {
                        return ticket.getAgent() != null && ticket.getAgent().getUserId() == userId;
                    } else {
                        return ticket.getCustomer() != null && ticket.getCustomer().getUserId() == userId;
                    }
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

    // Clear cache when tickets are updated
    @CacheEvict(value = "ticket_counts", allEntries = true)
    public void clearTicketCountsCache() {
        // Intentionally blank - annotation does the work
    }

    public TicketDTO searchTicket(long userId, long ticketId, int page, int size) {
        // Validate and retrieve the user
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Validate and retrieve the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException("Ticket not found with ID: " + ticketId));

        // Validate ticket ownership or association
        if (!ticket.getCustomer().equals(user) && (ticket.getAgent() == null || !ticket.getAgent().equals(user))) {
            throw new UserNotAuthorizedException("User ID " + userId + " is not authorized to view ticket ID " + ticketId);
        }

        // Get responses and paginate them
        List<TicketResponse> responses = Optional.ofNullable(ticket.getResponses())
                .orElse(Collections.emptyList());

        // Handle case where the requested page exceeds available data
        if (page < 0 || size <= 0 || page * size >= responses.size()) {
            return new TicketDTO(ticket.getTicketId(), ticket.getDescription(), ticket.getStatus().name(),
                    ticket.getCategory().name(), ticket.getCreatedAt(), ticket.getUpdatedAt(), Collections.emptyList());
        }

        // Paginate responses
        int start = page * size;
        int end = Math.min(start + size, responses.size());
        List<TicketResponse> paginatedResponses = responses.subList(start, end);

        // Map TicketResponses to TicketResponseDTOs
        List<TicketResponseDTO> responseDTOs = new ArrayList<>();
        for (TicketResponse response : paginatedResponses) {
            responseDTOs.add(new TicketResponseDTO(
                    response.getResponseId(),
                    ticket.getTicketId(),
                    response.getResponseText(),
                    response.getRole() != null ? response.getRole().toString() : "UNKNOWN",
                    response.getUser() != null ? response.getUser().getEmail() : "No Email",
                    ticket.getAgent() != null ? ticket.getAgent().getEmail() : ticket.getCustomer().getEmail(),
                    response.getCreatedAt()
            ));
        }

        // Return the ticket DTO with paginated responses
        return new TicketDTO(
                ticket.getTicketId(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getCategory().name(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                responseDTOs
        );
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

    public Ticket createTicket(long userId, String bookingId, String description, String role) {
        logger.info("Creating ticket for userId: " + userId + " with bookingId: " + bookingId);

        // Validate user existence
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Ensure only CUSTOMER can create a ticket
        if (!role.equalsIgnoreCase("CUSTOMER")) {
            throw new UserNotAuthorizedException("Only customers can create tickets.");
        }

        // Validate booking ID
        Booking booking = bookingRespository.findById(Long.parseLong(bookingId))
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        // Ensure user is associated with the booking
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new UserNotAuthorizedException("User is not authorized to access this booking.");
        }

        // Create ticket
        Ticket ticket = new Ticket();
        ticket.setCustomer(user);
        ticket.setBooking(booking);
        ticket.setDescription(description);
        ticket.setStatus(Status.ACTIVE);
        ticket.setCreatedAt(LocalDateTime.now());

        // Assign an agent
        User agent = assignAgentToTicket();
        ticket.setAgent(agent);

        logger.info("Ticket created successfully with ID: " + ticket.getTicketId());

        // Save and return ticket
        return ticketRepository.save(ticket);
    }

    private User assignAgentToTicket() {
        // Find agents with the role AGENT
        List<User> agents = userRespository.findByRole(Role.AGENT);

        if (agents.isEmpty()) {
            logger.error("No available agents for ticket assignment");
            throw new IllegalStateException("No available agents for ticket assignment");
        }

        // Round-robin agent selection

        User assignedAgent = agents.get((int) (count++ % agents.size()));
        logger.info("Assigned agent: " + assignedAgent.getEmail());
        return assignedAgent;
    }
}
