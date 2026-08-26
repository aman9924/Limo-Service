package com.honklimo.repository;

import com.honklimo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    // JOIN FETCH avoids LazyInitializationException when the admin dashboard template reads customer fields.
    @Query("SELECT b FROM Booking b JOIN FETCH b.customer c WHERE " +
            "(:q IS NULL OR :q = '' " +
            "OR LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR c.phone LIKE CONCAT('%', :q, '%')) " +
            "ORDER BY b.createdAt DESC")
    List<Booking> search(@Param("q") String q);
}
