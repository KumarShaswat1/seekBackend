package com.example.TicketApp.services;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.repository.UserRespository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private UserRespository userRepository;



    public User signup(UserSignRequestDTO userSignRequestDTO) {
        // Check if the user already exists by email
        Optional<User> existingUser = userRepository.findByEmail(userSignRequestDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Create and save new user
        User user = new User();
        user.setEmail(userSignRequestDTO.getEmail());
        user.setPassword(userSignRequestDTO.getPassword());  // Save password as plain text
        user.setRole(Role.valueOf(userSignRequestDTO.getRole().toUpperCase()));  // Convert role to Enum

        return userRepository.save(user);
    }


}
