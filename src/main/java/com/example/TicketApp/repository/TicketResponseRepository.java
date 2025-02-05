package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse,Long> {
    Optional<TicketResponse> findById(long responseId);


    @Query("SELECT tr FROM TicketResponse tr WHERE tr.ticket.id = :ticketId")
    Page<TicketResponse> findByTicketId(Long ticketId, Pageable pageable);

}
