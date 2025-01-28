package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {

}
