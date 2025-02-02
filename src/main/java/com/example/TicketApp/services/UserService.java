package com.example.TicketApp.services;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.constants.Constants;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.repository.UserRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRespository userRepository;

    // Constructor Injection
    public UserService(UserRespository userRepository) {
        this.userRepository = userRepository;
    }

    public User signup(UserSignRequestDTO userSignRequestDTO) {
        logger.info("Attempting to sign up user with email: {}", userSignRequestDTO.getEmail());

        // Check if the user already exists by email
        Optional<User> existingUser = userRepository.findByEmail(userSignRequestDTO.getEmail());
        if (existingUser.isPresent()) {
            logger.error("User with email {} already exists.", userSignRequestDTO.getEmail());
            throw new IllegalArgumentException(Constants.MESSAGE_USER_ALREADY_EXISTS);
        }

        // Create and save new user
        User user = new User();
        user.setEmail(userSignRequestDTO.getEmail());
        user.setPassword(userSignRequestDTO.getPassword());  // Save password as plain text
        try {
            user.setRole(Role.valueOf(userSignRequestDTO.getRole().toUpperCase()));  // Convert role to Enum
        } catch (IllegalArgumentException e) {
            logger.error("Invalid role: {}", userSignRequestDTO.getRole());
            throw new IllegalArgumentException(Constants.MESSAGE_INVALID_ROLE);
        }

        User savedUser = userRepository.save(user);
        logger.info("User with email {} created successfully.", userSignRequestDTO.getEmail());
        return savedUser;
    }
}
