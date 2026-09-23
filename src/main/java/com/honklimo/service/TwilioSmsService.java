package com.honklimo.service;

import com.honklimo.config.TwilioProperties;
import com.honklimo.entity.Booking;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    private final TwilioProperties twilioProperties;
    private boolean initialized = false;

    public TwilioSmsService(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    @PostConstruct
    public void init() {
        if (twilioProperties.getAccountSid() != null && !twilioProperties.getAccountSid().isBlank() &&
            twilioProperties.getAuthToken() != null && !twilioProperties.getAuthToken().isBlank()) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            initialized = true;
            log.info("Twilio SDK initialized for SMS.");
        } else {
            log.warn("Twilio credentials not fully provided. SMS Service is disabled.");
        }
    }

    public void sendBookingAcknowledgment(Booking booking) {
        if (!initialized || !twilioProperties.isSmsEnabled()) {
            return;
        }
        if (booking.getCustomer().getSmsConsent() == null || !booking.getCustomer().getSmsConsent()) {
            log.info("Skipping SMS acknowledgment for booking {} - no SMS consent.", booking.getBookingReference());
            return;
        }

        String to = formatPhone(booking.getCustomer().getPhone());
        String fareInfo = (booking.getEstimatedFare() != null && !booking.getEstimatedFare().trim().isEmpty() && !booking.getEstimatedFare().equalsIgnoreCase("null") && !booking.getEstimatedFare().equalsIgnoreCase("Call for Pricing")) 
            ? " (Estimated Fare: " + booking.getEstimatedFare() + ")" 
            : "";

        String msg = String.format(
            "Honk Limousine Service: We received your booking request %s for %s at %s%s. Your request is not yet confirmed. Our team will contact you to confirm availability and final details. Reply STOP to opt out or HELP for help.",
            booking.getBookingReference(),
            booking.getPickupDate(),
            booking.getPickupTime(),
            fareInfo
        );

        sendSmsSafely(to, msg, "Booking Acknowledgment");
    }

    public void sendBookingConfirmation(Booking booking) {
        if (!initialized || !twilioProperties.isSmsEnabled()) {
            return;
        }
        if (booking.getCustomer().getSmsConsent() == null || !booking.getCustomer().getSmsConsent()) {
            return;
        }

        String fareInfo = (booking.getEstimatedFare() != null && !booking.getEstimatedFare().trim().isEmpty() && !booking.getEstimatedFare().equalsIgnoreCase("null") && !booking.getEstimatedFare().equalsIgnoreCase("Call for Pricing")) 
            ? " Fare: " + booking.getEstimatedFare() + "." 
            : "";

        String to = formatPhone(booking.getCustomer().getPhone());
        String msg = String.format(
            "Honk Limousine Service: Your reservation %s has been confirmed for %s at %s. Pickup: %s.%s Reply STOP to opt out or HELP for help.",
            booking.getBookingReference(),
            booking.getPickupDate(),
            booking.getPickupTime(),
            booking.getPickupLocation(),
            fareInfo
        );

        sendSmsSafely(to, msg, "Booking Confirmation");
    }

    public void sendBookingCancellation(Booking booking) {
        if (!initialized || !twilioProperties.isSmsEnabled()) {
            return;
        }
        if (booking.getCustomer().getSmsConsent() == null || !booking.getCustomer().getSmsConsent()) {
            return;
        }

        String to = formatPhone(booking.getCustomer().getPhone());
        String msg = String.format(
            "Honk Limousine Service: Your booking request %s has been updated/cancelled. Please contact us if you have questions. Reply STOP to opt out or HELP for help.",
            booking.getBookingReference()
        );

        sendSmsSafely(to, msg, "Booking Cancellation");
    }

    public void sendOwnerNotification(Booking booking) {
        if (!initialized || !twilioProperties.isSmsEnabled()) {
            return;
        }
        if (twilioProperties.getOwnerSmsNumber() == null || twilioProperties.getOwnerSmsNumber().isBlank()) {
            return;
        }

        String fareInfo = (booking.getEstimatedFare() != null && !booking.getEstimatedFare().trim().isEmpty() && !booking.getEstimatedFare().equalsIgnoreCase("null") && !booking.getEstimatedFare().equalsIgnoreCase("Call for Pricing")) 
            ? " Fare: " + booking.getEstimatedFare() + "." 
            : "";

        String msg = String.format(
            "Honk Limousine Service: New booking request %s. Customer: %s. Phone: %s. Pickup: %s. Dropoff: %s. Date: %s. Time: %s. Vehicle: %s.%s Review the booking in the admin system.",
            booking.getBookingReference(),
            booking.getCustomer().getName(),
            booking.getCustomer().getPhone(),
            booking.getPickupLocation(),
            booking.getDropoffLocation(),
            booking.getPickupDate(),
            booking.getPickupTime(),
            booking.getVehicleType(),
            fareInfo
        );

        sendSmsSafely(twilioProperties.getOwnerSmsNumber(), msg, "Owner Notification");
    }

    private void sendSmsSafely(String to, String content, String type) {
        if (twilioProperties.getSmsFrom() == null || twilioProperties.getSmsFrom().isBlank()) {
            log.warn("Twilio sms-from is not configured, cannot send SMS.");
            return;
        }
        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioProperties.getSmsFrom()),
                    content
            ).create();
            log.info("Successfully sent {} SMS. SID: {}", type, message.getSid());
        } catch (Exception e) {
            String maskedPhone = maskPhone(to);
            log.error("Failed to send {} SMS to {}. Error: {}", type, maskedPhone, e.getMessage());
        }
    }

    private String formatPhone(String phone) {
        if (phone == null) return "";
        String clean = phone.replaceAll("[^0-9+]", "");
        if (!clean.startsWith("+")) {
            clean = "+1" + clean;
        }
        return clean;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
