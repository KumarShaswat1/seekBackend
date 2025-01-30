package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin("http://localhost:3000")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRespository userRespository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Signup the user
            User createdUser = userService.signup(userSignRequestDTO);
            response.put("status", "success");
            response.put("message", "User registered successfully");
            response.put("data", createdUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // 400 Bad Request
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if the user exists by email
            Optional<User> existingUser = userRespository.findByEmail(userSignRequestDTO.getEmail());
            if (!existingUser.isPresent()) {
                response.put("status", "error");
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
            }

            User user = existingUser.get();

            // Validate the password (plain text comparison)
            if (!user.getPassword().equals(userSignRequestDTO.getPassword())) {
                response.put("status", "error");
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401 Unauthorized
            }

            // Success response
            response.put("status", "success");
            response.put("message", "Login successful");

            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", user.getUserId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());

            response.put("data", userData);

            return ResponseEntity.ok(response); // 200 OK

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500 Internal Server Error
        }
    }

}


