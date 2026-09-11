package com.honklimo.service;

import com.honklimo.dto.BookingRequest;
import com.honklimo.entity.Booking;
import com.honklimo.entity.BookingStatus;
import com.honklimo.entity.Customer;
import com.honklimo.repository.BookingRepository;
import com.honklimo.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final TwilioSmsService twilioSmsService;

    public BookingService(CustomerRepository customerRepository, BookingRepository bookingRepository,
                          EmailService emailService, WhatsAppService whatsAppService, TwilioSmsService twilioSmsService) {
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.twilioSmsService = twilioSmsService;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        Customer customer = customerRepository.findByPhone(request.getPhone())
                .orElseGet(() -> new Customer(request.getFullName(), request.getPhone(), request.getEmail()));
                
        // Only update SMS consent if they provided it (we don't want to overwrite an opt-in with an opt-out implicitly,
        // though typically web forms are a hard overwrite. Since it's a new request, if they checked it, set it).
        if (request.isSmsConsent()) {
            customer.setSmsConsent(true);
            customer.setSmsConsentTimestamp(java.time.Instant.now());
        }
        
        customer = customerRepository.save(customer);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setServiceType(request.getServiceType());
        booking.setVehicleType(request.getVehicleType());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDropoffLocation(request.getDropoffLocation());
        booking.setPickupDate(request.getPickupDate());
        booking.setPickupTime(request.getPickupTime());
        booking.setReturnDate(request.getReturnDate());
        booking.setReturnTime(request.getReturnTime());
        booking.setPassengers(request.getPassengers());
        booking.setLuggage(request.getLuggage());
        booking.setFlightNumber(request.getFlightNumber());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setMeetAndGreet(request.isMeetAndGreet());
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);
        saved.setBookingReference(generateReference(saved.getId()));
        return bookingRepository.save(saved);
    }

    private String generateReference(Long id) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "HONK-" + datePart + "-" + String.format("%06d", id);
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findForTracking(String bookingReference, String phone) {
        return bookingRepository.findByBookingReference(bookingReference.trim())
                .filter(b -> normalizePhone(b.getCustomer().getPhone()).equals(normalizePhone(phone)));
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference.trim());
    }

    @Transactional
    public Booking cancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("This booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed bookings cannot be cancelled.");
        }
        if (isWithinCancellationWindow(booking)) {
            throw new IllegalStateException(
                    "Cancellation window has passed (within 24 hours of pickup). Please call us directly.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    private boolean isWithinCancellationWindow(Booking booking) {
        try {
            LocalDate date = LocalDate.parse(booking.getPickupDate());
            LocalTime time = LocalTime.parse(booking.getPickupTime());
            return LocalDateTime.of(date, time).isBefore(LocalDateTime.now().plusHours(24));
        } catch (Exception e) {
            // If the stored date/time can't be parsed, don't block a legitimate cancellation.
            return false;
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    // ---- Admin operations ----

    @Transactional(readOnly = true)
    public List<Booking> searchForAdmin(String query) {
        return bookingRepository.search(query == null ? "" : query.trim());
    }

    @Transactional
    public Booking updateBooking(Long id, BookingStatus status, String driverName, String driverPhone) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        booking.setDriverName(driverName);
        booking.setDriverPhone(driverPhone);
        
        Booking saved = bookingRepository.save(booking);

        if (oldStatus != BookingStatus.CONFIRMED && status == BookingStatus.CONFIRMED) {
            try {
                emailService.sendBookingConfirmation(saved);
                whatsAppService.sendBookingConfirmation(saved);
                twilioSmsService.sendBookingConfirmation(saved);
            } catch (Exception e) {
                // Log and continue, don't fail transaction
            }
        } else if (oldStatus != BookingStatus.CANCELLED && status == BookingStatus.CANCELLED) {
            try {
                emailService.sendBookingCancellation(saved);
                whatsAppService.sendBookingCancellation(saved);
                twilioSmsService.sendBookingCancellation(saved);
            } catch (Exception e) {
                // Log and continue
            }
        }
        
        return saved;
    }

    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
}
