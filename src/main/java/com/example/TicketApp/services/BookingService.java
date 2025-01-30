package com.example.TicketApp.services;

import com.example.TicketApp.CustomErrors.BookingNotFoundException;
import com.example.TicketApp.CustomErrors.UserNotAuthorizedException;
import com.example.TicketApp.CustomErrors.UserNotFoundException;
import com.example.TicketApp.entity.Booking;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.BookingRespository;
import com.example.TicketApp.repository.UserRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    @Autowired
    private BookingRespository bookingRepository;

    @Autowired
    private UserRespository userRepository;

    public boolean validateBooking(long userId, long bookingId) {
        // Fetch user using orElseThrow for cleaner code
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found"));

        // Fetch booking using orElseThrow
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        // Check if the user is associated with the booking
        if (booking.getUser () == null || !booking.getUser().getUserId().equals(user.getUserId())) {
            throw new UserNotAuthorizedException("User  is not authorized to access this booking");
        }

        return true;
    }
}

