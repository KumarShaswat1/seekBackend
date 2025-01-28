package com.example.TicketApp.repository;

import com.example.TicketApp.entity.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse,Long> {
}
