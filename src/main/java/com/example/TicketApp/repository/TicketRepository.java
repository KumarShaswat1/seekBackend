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


     List<Ticket> findByAgentIdAndStatus(long userId, Status ticketStatus);

     List<Ticket> findByCustomerIdAndStatus(long userId, Status ticketStatus);

     List<Ticket> findByAgentIdAndCategory(long userId, String category);

     List<Ticket> findByCustomerIdAndCategory(long userId, String category);
}
