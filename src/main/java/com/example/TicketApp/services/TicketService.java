package com.example.TicketApp.services;

import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Category;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.enums.Status;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service

public class TicketService {



    @Autowired
    private UserRespository userRespository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketResponseRepository ticketResponseRepository;

    public Page<SimpleTicketDTO> getFilteredTickets(long userId, String role, String status, String category, int page, int size) {
        // Validate user existence
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User  not found with ID: " + userId));

        List<Ticket> tickets;

        // Determine tickets based on role
        if ("AGENT".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsAgent();
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsCustomer();
        } else {
            throw new IllegalArgumentException("Invalid role. Must be 'AGENT' or 'CUSTOMER'.");
        }

        // Manually filter tickets based on status and category
        List<SimpleTicketDTO> filteredTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (filterTickets(ticket, status, category)) {
                filteredTickets.add(new SimpleTicketDTO(
                        ticket.getTicketId(),
                        ticket.getDescription(),
                        ticket.getStatus().name(),
                        ticket.getCategory().name(),
                        ticket.getCreatedAt(),
                        ticket.getUpdatedAt(),
                        ticket.getAgent() != null ? ticket.getAgent().getEmail() : null,
                        ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : null
                ));
            }
        }

        // Calculate start and end indices for pagination
        int start = page * size;
        int end = Math.min(start + size, filteredTickets.size());

        // Handle case where the requested page exceeds available data
        if (start >= filteredTickets.size()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), filteredTickets.size());
        }

        // Create a sublist for the current page
        List<SimpleTicketDTO> paginatedList = filteredTickets.subList(start, end);

        // Return a Page object
        return new PageImpl<>(paginatedList, PageRequest.of(page, size), filteredTickets.size());
    }

    // Helper method for filtering tickets based on status and category
    private boolean filterTickets(Ticket ticket, String status, String category) {
        return ("ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(ticket.getStatus().name())) &&
                ("ALL".equalsIgnoreCase(category) || category.equalsIgnoreCase(ticket.getCategory().name()));
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "ticket_counts::";
    private static final long CACHE_TTL = 30; // 30 minutes

    public Map<String, Long> getCountActiveResolved(long userId, String role, String category) {
        // Validate role
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }

        // Generate cache key
        String cacheKey = CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category;

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
        redisTemplate.opsForValue().set(cacheKey, counts, Duration.ofMinutes(CACHE_TTL));

        return counts;
    }

    // Clear cache when tickets are updated
    @CacheEvict(value = "ticket_counts", allEntries = true)
    public void clearTicketCountsCache() {
        // Intentionally blank - annotation does the work
    }
    // A helper method to filter tickets based on status and category
    private boolean filterTicketsCategory(Ticket ticket, String status, String category) {
        if ("ALL".equalsIgnoreCase(category)) {
            return ticket.getStatus().name().equalsIgnoreCase(status);
        }
        return ticket.getStatus().name().equalsIgnoreCase(status) &&
                ticket.getCategory().name().equalsIgnoreCase(category);
    }


    public TicketDTO searchTicket(long userId, long ticketId, int page, int size) {
        // Validate and retrieve the user
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User  not found with ID: " + userId));

        // Validate and retrieve the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));

        // Validate ticket ownership or association
        if (!ticket.getCustomer().equals(user) && (ticket.getAgent() == null || !ticket.getAgent().equals(user))) {
            throw new IllegalArgumentException("User  ID " + userId + " is not authorized to view ticket ID " + ticketId);
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
                    response.getUser () != null ? response.getUser ().getEmail() : "No Email",
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
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));


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



    private static final Logger logger = Logger.getLogger(TicketService.class.getName());
    private static long count = 0;

    public Ticket createTicket(long userId, String category,String role,long booking_id, String description) {
        logger.info("Starting to create ticket for userId: " + userId + " with category: " + category);

        try {
            // Validate if the customer exists
            User customer = userRespository.findById(userId)
                    .orElseThrow(() -> {
                        logger.severe("User not found for ID: " + userId);
                        return new IllegalArgumentException("User not found");
                    });

            logger.info("User found: " + customer.getEmail());

            if (!customer.getRole().equals(Role.CUSTOMER)) {
                logger.severe("User with ID: " + userId + " is not a customer.");
                throw new IllegalArgumentException("Only customers can create tickets");
            }

            // Validate the category
            if (!category.equalsIgnoreCase("prebooking") && !category.equalsIgnoreCase("postbooking")) {
                logger.severe("Invalid category: " + category);
                throw new IllegalArgumentException("Invalid category. Must be 'prebooking' or 'postbooking'");
            }

            // Create the ticket
            Ticket ticket = new Ticket();
            ticket.setCustomer(customer);
            ticket.setCategory(Category.valueOf(category.toUpperCase()));
            ticket.setStatus(Status.ACTIVE);
            ticket.setDescription(description);

            // Assign an agent to the ticket
            User agent = assignAgentToTicket();
            ticket.setAgent(agent);

            // Save the ticket to the repository
            ticket = ticketRepository.save(ticket);
            logger.info("Ticket created with ID: " + ticket.getTicketId());

            return ticket;
        } catch (IllegalArgumentException e) {
            logger.severe("Argument exception occurred: " + e.getMessage());
            throw e; // Rethrow for further handling in the controller
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Internal server error while creating ticket", e);
        }
    }

    private User assignAgentToTicket() {
        // Find agents with the role AGENT
        List<User> agents = userRespository.findByRole(Role.AGENT);

        if (agents.isEmpty()) {
            logger.severe("No available agents for ticket assignment");
            throw new IllegalStateException("No available agents for ticket assignment");
        }

        // Round-robin agent selection
        User assignedAgent = agents.get((int) (count++ % agents.size()));
        logger.info("Assigned agent: " + assignedAgent.getEmail());
        return assignedAgent;
    }



}
