package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse,Long> {
    Optional<TicketResponse> findById(long responseId);


    Page<TicketResponse> findByTicket(@Param("ticket") Ticket ticket, Pageable pageable);

}
