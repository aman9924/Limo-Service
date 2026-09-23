package com.honklimo.controller;

import com.honklimo.dto.TrackRequest;
import com.honklimo.entity.Booking;
import com.honklimo.service.BookingService;
import com.honklimo.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Guest-facing lookup — no login required. Verified with booking reference + phone (not reference alone).
@RestController
@RequestMapping("/api/bookings")
public class BookingTrackController {

    private final BookingService bookingService;
    private final RateLimiterService rateLimiterService;

    public BookingTrackController(BookingService bookingService, RateLimiterService rateLimiterService) {
        this.bookingService = bookingService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> track(@Valid @RequestBody TrackRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiterService.allow("track:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(error("Too many attempts. Please try again in a few minutes."));
        }

        Optional<Booking> found = bookingService.findForTracking(request.getBookingReference(), request.getPhone());
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("No booking found matching that reference and phone number."));
        }

        return ResponseEntity.ok(bookingSummary(found.get()));
    }

    @PostMapping("/track/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@Valid @RequestBody TrackRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiterService.allow("cancel:" + httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(error("Too many attempts. Please try again in a few minutes."));
        }

        Optional<Booking> found = bookingService.findForTracking(request.getBookingReference(), request.getPhone());
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("No booking found matching that reference and phone number."));
        }

        try {
            Booking cancelled = bookingService.cancelBooking(found.get());
            Map<String, Object> body = bookingSummary(cancelled);
            body.put("message", "Your booking has been cancelled.");
            return ResponseEntity.ok(body);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, Object> bookingSummary(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "success");
        m.put("bookingReference", b.getBookingReference());
        m.put("bookingStatus", b.getStatus().name());
        m.put("serviceType", b.getServiceType());
        m.put("vehicleType", b.getVehicleType());
        m.put("pickupLocation", b.getPickupLocation());
        m.put("dropoffLocation", b.getDropoffLocation());
        m.put("pickupDate", b.getPickupDate());
        m.put("pickupTime", b.getPickupTime());
        m.put("estimatedFare", b.getEstimatedFare());
        m.put("meetAndGreet", b.isMeetAndGreet());
        m.put("driverName", b.getDriverName());
        m.put("driverPhone", b.getDriverPhone());
        return m;
    }

    private Map<String, Object> error(String message) {
        return Map.of("status", "error", "message", message);
    }
}
