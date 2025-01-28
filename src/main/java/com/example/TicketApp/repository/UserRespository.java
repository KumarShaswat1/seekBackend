package com.example.TicketApp.repository;

import com.example.TicketApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRespository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
}
