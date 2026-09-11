package com.honklimo.controller;

import com.honklimo.dto.BookingRequest;
import com.honklimo.entity.Booking;
import com.honklimo.service.BookingService;
import com.honklimo.service.EmailService;
import com.honklimo.service.WhatsAppService;
import com.honklimo.service.TwilioSmsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingApiController {

    private static final Logger log = LoggerFactory.getLogger(BookingApiController.class);

    private final WhatsAppService whatsAppService;
    private final BookingService bookingService;
    private final EmailService emailService;
    private final TwilioSmsService twilioSmsService;

    public BookingApiController(WhatsAppService whatsAppService, BookingService bookingService, EmailService emailService, TwilioSmsService twilioSmsService) {
        this.whatsAppService = whatsAppService;
        this.bookingService = bookingService;
        this.emailService = emailService;
        this.twilioSmsService = twilioSmsService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, String>> submitBooking(@Valid @RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            
            // Notifications are designed not to fail the main booking transaction
            whatsAppService.sendBookingNotifications(request);
            emailService.sendBookingNotifications(booking);
            
            // New SMS A2P 10DLC notifications
            twilioSmsService.sendBookingAcknowledgment(booking);
            twilioSmsService.sendOwnerNotification(booking);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Thank you! Your booking request has been sent.",
                    "bookingReference", booking.getBookingReference()
            ));
        } catch (Exception e) {
            log.error("Unexpected error while processing booking", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Something went wrong. Please call or WhatsApp us directly."
            ));
        }
    }

    @GetMapping(value = "/bookings/{ref}/calendar", produces = "text/calendar")
    public ResponseEntity<String> getCalendarEvent(@PathVariable String ref) {
        return bookingService.findByReference(ref).map(booking -> {
            String dtStart = "";
            String dtEnd = "";
            try {
                LocalDateTime start = LocalDateTime.parse(booking.getPickupDate() + "T" + booking.getPickupTime());
                dtStart = "DTSTART:" + start.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n";
                dtEnd = "DTEND:" + start.plusHours(1).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n";
            } catch (Exception e) {
                // Fallback if parsing fails
            }

            String driverText = (booking.getDriverName() != null && !booking.getDriverName().isBlank()) 
                                ? "\\nDriver: " + booking.getDriverName() + (booking.getDriverPhone() != null ? " (" + booking.getDriverPhone() + ")" : "") 
                                : "";

            String ics = "BEGIN:VCALENDAR\n" +
                    "VERSION:2.0\n" +
                    "PRODID:-//HONK Limousine Service//EN\n" +
                    "BEGIN:VEVENT\n" +
                    "UID:" + booking.getBookingReference() + "@honklimo.com\n" +
                    dtStart +
                    dtEnd +
                    "SUMMARY:HONK Limo Ride\n" +
                    "DESCRIPTION:Booking Ref: " + booking.getBookingReference() + "\\nPickup: " + booking.getPickupLocation() + "\\nDrop-off: " + booking.getDropoffLocation() + driverText + "\n" +
                    "LOCATION:" + booking.getPickupLocation() + "\n" +
                    "END:VEVENT\n" +
                    "END:VCALENDAR";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"honk-ride-" + booking.getBookingReference() + ".ics\"")
                    .body(ics);
        }).orElse(ResponseEntity.notFound().build());
    }
}
