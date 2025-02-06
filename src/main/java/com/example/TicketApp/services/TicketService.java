package com.example.TicketApp.services;

import com.example.TicketApp.CustomErrors.BookingNotFoundException;
import com.example.TicketApp.CustomErrors.UserNotAuthorizedException;
import com.example.TicketApp.CustomErrors.UserNotFoundException;
import com.example.TicketApp.DTO.*;
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

    public Map<String, Long> getCountActiveResolved(long userId, String role, String category) {
        // Validate role
        if (role == null ||
                !(role.equalsIgnoreCase(Constants.ROLE_AGENT) || role.equalsIgnoreCase(Constants.ROLE_CUSTOMER))) {
            throw new IllegalArgumentException(Constants.MESSAGE_INVALID_ROLE);
        }

        // Initialize ticket size variable
        long ticketSize = 0;

        // Generate cache keys for active and resolved counts
        String cacheKeyActive;
        String cacheKeyResolved;

        long activeCount;
        long resolvedCount;

        // If category is "ALL", make a DB call to fetch counts for all tickets
        if (Constants.STATUS_ALL.equalsIgnoreCase(category)) {
            activeCount = ticketRepository.countByStatusAndCategoryAndUserId(Status.ACTIVE, null, userId, role);
            resolvedCount = ticketRepository.countByStatusAndCategoryAndUserId(Status.RESOLVED, null, userId, role);
            ticketSize = ticketRepository.countByUserId(userId, role);  // Fetch the total size of tickets for the user

            // Generate the cache key by adding ticket size
            cacheKeyActive = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category + "::ACTIVE::" + ticketSize;
            cacheKeyResolved = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category + "::RESOLVED::" + ticketSize;

        } else {
            // Handle specific categories
            Category ticketCategory;
            switch (category.toLowerCase()) {
                case "prebooking":
                    ticketCategory = Category.PREBOOKING;
                    break;
                case "postbooking":
                    ticketCategory = Category.POSTBOOKING;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid category: " + category);
            }

            activeCount = ticketRepository.countByStatusAndCategoryAndUserId(Status.ACTIVE, ticketCategory, userId, role);
            resolvedCount = ticketRepository.countByStatusAndCategoryAndUserId(Status.RESOLVED, ticketCategory, userId, role);
            ticketSize = ticketRepository.countByCategoryAndUserId(ticketCategory, userId, role);  // Fetch the ticket size for the category

            // Generate the cache key by adding ticket size
            cacheKeyActive = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category + "::ACTIVE::" + ticketSize;
            cacheKeyResolved = Constants.CACHE_KEY_PREFIX + userId + "::" + role.toUpperCase() + "::" + category + "::RESOLVED::" + ticketSize;
        }

        // Try to get cached result for active and resolved counts
        Long cachedActiveCount = (Long) redisTemplate.opsForValue().get(cacheKeyActive);
        Long cachedResolvedCount = (Long) redisTemplate.opsForValue().get(cacheKeyResolved);

        // If both counts are cached, return them
        if (cachedActiveCount != null && cachedResolvedCount != null) {
            Map<String, Long> counts = new HashMap<>();
            counts.put(Constants.STATUS_ACTIVE, cachedActiveCount);
            counts.put(Constants.STATUS_RESOLVED, cachedResolvedCount);
            return counts;
        }

        // Prepare counts map
        Map<String, Long> counts = new HashMap<>();
        counts.put(Constants.STATUS_ACTIVE, activeCount);
        counts.put(Constants.STATUS_RESOLVED, resolvedCount);

        // Cache the results for active and resolved counts with TTL
        redisTemplate.opsForValue().set(cacheKeyActive, activeCount, Duration.ofMinutes(Constants.CACHE_TTL));
        redisTemplate.opsForValue().set(cacheKeyResolved, resolvedCount, Duration.ofMinutes(Constants.CACHE_TTL));

        return counts;
    }

    public Map<String, Object> searchTicket(long userId, long ticketId, int page, int size) {
        Map<String, Object> responseMap = new HashMap<>();

        // Validate and retrieve the user
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.format(Constants.LOG_USER_NOT_FOUND, userId)));

        // Validate and retrieve the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException(String.format(Constants.LOG_TICKET_NOT_FOUND, ticketId)));

        // Validate ticket ownership or association (Customer or Agent)
        if (!ticket.getCustomer().equals(user) && (ticket.getAgent() == null || !ticket.getAgent().equals(user))) {
            throw new UserNotAuthorizedException(String.format(Constants.LOG_USER_NOT_AUTHORIZED, userId, ticketId));
        }

        // Map the ticket fields into the response
        Map<String, Object> ticketDetails = new HashMap<>();
        ticketDetails.put("ticketId", ticket.getTicketId());
        ticketDetails.put("status", ticket.getStatus());
        ticketDetails.put("category", ticket.getCategory());
        ticketDetails.put("time", ticket.getCreatedAt());
        ticketDetails.put("description", ticket.getDescription());

        // Create a Pageable object for pagination
        Pageable pageable = PageRequest.of(page, size);

        // Fetch paginated responses directly from the repository
        Page<TicketResponse> paginatedResponsePage = ticketResponseRepository.findByTicketId(ticketId, pageable);

        // Get the content of the paginated responses
        List<TicketResponse> paginatedResponses = paginatedResponsePage.getContent();

        // Calculate total responses for pagination
        long totalResponses = paginatedResponsePage.getTotalElements();
        int totalPages = paginatedResponsePage.getTotalPages();

        // Map the paginated responses to DTOs
        List<TicketResponseDTO> mergedDTOs = mapResponsesToDTOs(paginatedResponses, ticket, user);

        // Add ticket details, responses, and total pages to the response map
        ticketDetails.put("responses", mergedDTOs);
        ticketDetails.put("totalPages", totalPages);

        return ticketDetails;
    }

    private List<TicketResponseDTO> mapResponsesToDTOs(List<TicketResponse> responses, Ticket ticket, User user) {
        List<TicketResponseDTO> responseDTOs = new ArrayList<>();
        for (TicketResponse response : responses) {
            String userEmail;
            String agentEmail;

            // For CUSTOMER: userEmail is the customer's email, and agentEmail is the agent's email
            // For AGENT: userEmail is the agent's email, and agentEmail is the customer's email
            if (user.getRole() == null || user.getRole() == Role.CUSTOMER) {
                userEmail = ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : Constants.NO_EMAIL;
                agentEmail = ticket.getAgent() != null ? ticket.getAgent().getEmail() : Constants.NO_EMAIL;
            } else if (user.getRole() == Role.AGENT) {
                userEmail = ticket.getAgent() != null ? ticket.getAgent().getEmail() : Constants.NO_EMAIL;
                agentEmail = ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : Constants.NO_EMAIL;
            } else {
                // Default case if the role is null or unrecognized
                userEmail = Constants.NO_EMAIL;
                agentEmail = Constants.NO_EMAIL;
            }

            // Add the response to DTO list
            responseDTOs.add(new TicketResponseDTO(
                    response.getResponseId(),
                    ticket.getTicketId(),
                    response.getResponseText(),
                    response.getRole() != null ? response.getRole().toString() : Constants.UNKNOWN,
                    userEmail,        // from
                    agentEmail,       // to
                    response.getCreatedAt()
            ));
        }

        return responseDTOs;
    }

    public Ticket createTicket(long userId, Long bookingId, String description, String role) {
        logger.info(String.format(Constants.LOG_USER_SIGNUP_ATTEMPT, userId));

        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.format(Constants.LOG_USER_NOT_FOUND, userId)));

        if (!role.equalsIgnoreCase(Constants.ROLE_CUSTOMER)) {
            throw new UserNotAuthorizedException(Constants.MESSAGE_INVALID_ROLE);
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
                    .orElseThrow(() -> new BookingNotFoundException(String.format(Constants.MESSAGE_TICKET_NOT_FOUND, bookingId)));

            if (!booking.getUser().getUserId().equals(user.getUserId())) {
                throw new UserNotAuthorizedException("User is not authorized to access this booking.");
            }

            ticket.setCategory(Category.POSTBOOKING);
            ticket.setBooking(booking);
        }

        User agent = assignAgentToTicket();
        ticket.setAgent(agent);

        Ticket savedTicket = ticketRepository.save(ticket);
        logger.info(String.format(Constants.LOG_USER_CREATED, savedTicket.getTicketId()));

        return savedTicket;
    }

    private User assignAgentToTicket() {
        List<User> agents = userRespository.findByRole(Role.AGENT);
        if (agents.isEmpty()) {
            throw new IllegalStateException("No available agents for ticket assignment");
        }
        return agents.get(new Random().nextInt(agents.size()));
    }
    public Map<String, List<SimpleTicketDTO>> getFilteredTickets(long userId, String role, String status, String bookingCategory, Pageable pageable) {
        // Validate the role
        if (role == null || (!role.equalsIgnoreCase(Constants.ROLE_AGENT) && !role.equalsIgnoreCase(Constants.ROLE_CUSTOMER))) {
            throw new IllegalArgumentException(Constants.MESSAGE_INVALID_ROLE);
        }

        // Validate booking category
        if (!"prebooking".equalsIgnoreCase(bookingCategory) && !"postbooking".equalsIgnoreCase(bookingCategory)) {
            throw new IllegalArgumentException("Invalid booking category: " + bookingCategory);
        }

        // Handle the "ALL" status case
        Page<Ticket> ticketPage;
        if (Constants.STATUS_ALL.equalsIgnoreCase(status)) {
            // Fetch tickets without filtering by status if status is "ALL"
            ticketPage = ticketRepository.findAllTicketsWithoutStatusAndBookingCategory(userId, Role.valueOf(role.toUpperCase()), bookingCategory, pageable);
        } else {
            // Validate and convert status to enum
            Status statusEnum;
            try {
                statusEnum = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            // Fetch tickets with status and booking category filtering
            ticketPage = ticketRepository.findByUserIdAndRoleAndStatusAndBookingCategory(userId, Role.valueOf(role.toUpperCase()), statusEnum, bookingCategory, pageable);
        }

        List<Ticket> tickets = ticketPage.getContent();

        // Create a list to hold the filtered tickets
        List<SimpleTicketDTO> filteredTickets = new ArrayList<>();

        for (Ticket ticket : tickets) {
            // Get Customer and Agent emails
            String customerEmail = (ticket.getCustomer() != null) ? ticket.getCustomer().getEmail() : Constants.NO_EMAIL;
            String agentEmail = (ticket.getAgent() != null) ? ticket.getAgent().getEmail() : Constants.NO_EMAIL;

            // Create DTO with email addresses included
            SimpleTicketDTO dto = new SimpleTicketDTO(
                    ticket.getTicketId(),
                    ticket.getDescription(),
                    ticket.getStatus().name(),
                    ticket.getCreatedAt(),
                    customerEmail,
                    agentEmail
            );

            // Add the ticket DTO to the list
            filteredTickets.add(dto);
        }

        // Create a map to hold the filtered tickets
        Map<String, List<SimpleTicketDTO>> result = new HashMap<>();
        result.put(bookingCategory.equalsIgnoreCase("prebooking") ? "PrebookingTickets" : "PostbookingTickets", filteredTickets);

        return result;
    }

    public List<TicketResponseDTO> getAllTicketResponses(long userId, long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BookingNotFoundException("Ticket not found with ID: " + ticketId));

        List<TicketResponse> ticketResponses = ticket.getResponses();
        List<TicketResponseDTO> repliesDTO = new ArrayList<>();
        for (TicketResponse ticketResponse : ticketResponses) {
            User user = ticketResponse.getUser ();

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    ticketResponse.getResponseId(),                // Response ID
                    ticket.getTicketId(),                          // Associated Ticket ID
                    ticketResponse.getResponseText(),              // Response Text
                    ticketResponse.getRole().toString(),           // Role
                    ticketResponse.getUser ().getEmail(),           // User's Email
                    ticket.getAgent() != null ? ticket.getAgent().getEmail() : null, // Agent's Email
                    ticketResponse.getCreatedAt()                  // Created At Timestamp
            );

            repliesDTO.add(responseDTO);
        }

        return repliesDTO;
    }

}
