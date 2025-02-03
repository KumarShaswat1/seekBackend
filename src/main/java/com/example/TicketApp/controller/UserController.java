package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.constants.Constants;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@CrossOrigin("http://localhost:3000")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserRespository userRespository;

    public UserController(UserService userService, UserRespository userRespository) {
        this.userService = userService;
        this.userRespository = userRespository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("User signup requested for email: {}", userSignRequestDTO.getEmail());
            User createdUser = userService.signup(userSignRequestDTO);
            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", Constants.MESSAGE_USER_REGISTERED_SUCCESSFULLY);
            response.put("data", createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error during signup: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Internal server error during signup: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Login requested for email: {}", userSignRequestDTO.getEmail());
            Optional<User> existingUser = userRespository.findByEmail(userSignRequestDTO.getEmail());

            if (!existingUser.isPresent()) {
                response.put("status", Constants.STATUS_ERROR);
                response.put("message", Constants.MESSAGE_USER_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User user = existingUser.get();

            // Validate Password
            if (!user.getPassword().equals(userSignRequestDTO.getPassword())) {
                response.put("status", Constants.STATUS_ERROR);
                response.put("message", Constants.MESSAGE_INVALID_USERNAME_OR_PASSWORD);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Check if the role in DTO matches the role in the database for this user
            if (!hasValidRole(userSignRequestDTO.getRole(), String.valueOf(user.getRole()))) {
                response.put("status", Constants.STATUS_ERROR);
                response.put("message", "Access denied: User does not have the required role.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            response.put("status", Constants.STATUS_SUCCESS);
            response.put("message", Constants.MESSAGE_LOGIN_SUCCESSFUL);

            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", user.getUserId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());
            response.put("data", userData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Internal server error during login: {}", e.getMessage());
            response.put("status", Constants.STATUS_ERROR);
            response.put("message", Constants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper method to check if the role from DTO matches the user's role from the database
    private boolean hasValidRole(String roleFromDTO, String userRole) {
        // Check if role from DTO is valid and matches the user's role in the database
        return roleFromDTO != null && (roleFromDTO.equals("CUSTOMER") || roleFromDTO.equals("AGENT"))
                && roleFromDTO.equals(userRole);
    }

}


