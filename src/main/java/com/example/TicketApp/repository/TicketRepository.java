package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Role;
import com.example.TicketApp.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {

     Optional<Ticket> findById(long ticketId);

          @Query("SELECT t FROM Ticket t " +
                  "WHERE (:userId IS NULL OR t.customer.userId = :userId OR t.agent.userId = :userId) " +
                  "AND (:role IS NULL OR t.customer.role = :role OR t.agent.role = :role) " +
                  "AND (:status IS NULL OR t.status = :status)")
          Page<Ticket> findByUserIdAndRoleAndStatus(
                  @Param("userId") Long userId,
                  @Param("role") Role role,
                  @Param("status") Status status,
                  Pageable pageable);
     }
