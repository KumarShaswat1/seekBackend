package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRespository extends JpaRepository<Booking,Long> {
}
