package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.User;
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

     @Query("SELECT t FROM Ticket t WHERE " +
             "(:role = 'AGENT' AND t.agent.id = :userId) OR " +
             "(:role = 'CUSTOMER' AND t.customer.id = :userId) AND " +
             "(:status IS NULL OR t.status = :status)")
     Page<Ticket> findFilteredTickets(@Param("userId") long userId,
                                      @Param("role") String role,
                                      @Param("status") String status,
                                      Pageable pageable);
}
