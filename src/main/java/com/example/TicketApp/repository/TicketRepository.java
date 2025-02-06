package com.example.TicketApp.repository;

import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.enums.Category;
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
public interface TicketRepository extends JpaRepository<Ticket, Long> {

     Optional<Ticket> findById(long ticketId);

     @Query("SELECT t FROM Ticket t " +
             "WHERE (:userId IS NULL OR t.customer.userId = :userId OR t.agent.userId = :userId) " +
             "AND (:role IS NULL OR t.customer.role = :role OR t.agent.role = :role) " +
             "AND (:status IS NULL OR t.status = :status) " +
             "AND (:bookingCategory IS NULL OR (t.booking IS NULL AND :bookingCategory = 'prebooking') OR (t.booking IS NOT NULL AND :bookingCategory = 'postbooking'))")
     Page<Ticket> findByUserIdAndRoleAndStatusAndBookingCategory(
             @Param("userId") Long userId,
             @Param("role") Role role,
             @Param("status") Status status,
             @Param("bookingCategory") String bookingCategory,
             Pageable pageable);

     @Query("SELECT t FROM Ticket t " +
             "WHERE (:userId IS NULL OR t.customer.userId = :userId OR t.agent.userId = :userId) " +
             "AND (:role IS NULL OR t.customer.role = :role OR t.agent.role = :role) " +
             "AND (:bookingCategory IS NULL OR (t.booking IS NULL AND :bookingCategory = 'prebooking') OR (t.booking IS NOT NULL AND :bookingCategory = 'postbooking'))")
     Page<Ticket> findAllTicketsWithoutStatusAndBookingCategory(
             @Param("userId") Long userId,
             @Param("role") Role role,
             @Param("bookingCategory") String bookingCategory,
             Pageable pageable);


          @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status AND " +
                  "((:role = 'AGENT' AND t.agent.userId = :userId) OR (:role = 'CUSTOMER' AND t.customer.userId = :userId)) " +
                  "AND (:category IS NULL OR t.category = :category)")
          long countByStatusAndCategoryAndUserId(@Param("status") Status status,
                                                  @Param("category") Category category,
                                                  @Param("userId") long userId,
                                                  @Param("role") String role);



     // Count tickets by user ID and role (for "ALL" category)
     @Query("SELECT COUNT(t) FROM Ticket t WHERE " +
             "((:role = 'AGENT' AND t.agent.userId = :userId) OR (:role = 'CUSTOMER' AND t.customer.userId = :userId))")
     long countByUserId(@Param("userId") long userId, @Param("role") String role);

     // Count tickets by category and user ID
     @Query("SELECT COUNT(t) FROM Ticket t WHERE t.category = :category AND " +
             "((:role = 'AGENT' AND t.agent.userId = :userId) OR (:role = 'CUSTOMER' AND t.customer.userId = :userId))")
     long countByCategoryAndUserId(@Param("category") Category category,
                                    @Param("userId") long userId,
                                    @Param("role") String role);

}