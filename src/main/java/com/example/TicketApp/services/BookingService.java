package com.example.TicketApp.services;

import com.example.TicketApp.entity.Booking;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.BookingRespository;
import com.example.TicketApp.repository.UserRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRespository bookingRepository;

    @Autowired
    private UserRespository userRepository;

    public boolean validateBooking(long userId, long bookingId) {

        Optional<User> userFound = userRepository.findById(userId);
        if (!userFound.isPresent()) {
            throw new IllegalArgumentException("User not found");
        }


        Optional<Booking> bookingFound = bookingRepository.findById(bookingId);
        if (!bookingFound.isPresent()) {
            throw new IllegalArgumentException("Booking not found");
        }


        Booking booking = bookingFound.get();
        User user = userFound.get();

        if (!booking.getUser().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to access this booking");
        }

        return true;
    }
}
