package com.honklimo.service;

import com.honklimo.config.TwilioProperties;
import com.honklimo.dto.BookingRequest;
import com.honklimo.entity.Booking;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final TwilioProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean initialized = false;

    public WhatsAppService(TwilioProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (StringUtils.hasText(props.getAccountSid()) && StringUtils.hasText(props.getAuthToken())) {
            Twilio.init(props.getAccountSid(), props.getAuthToken());
            initialized = true;
            log.info("Twilio client initialized.");
        } else {
            log.warn("Twilio credentials are not configured — WhatsApp notifications are disabled. " +
                    "Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN environment variables to enable them.");
        }
    }

    public void sendBookingNotifications(BookingRequest booking) {
        if (!initialized) {
            log.warn("Skipping WhatsApp notification — Twilio is not configured.");
            return;
        }

        if (StringUtils.hasText(props.getOwnerWhatsappTo()) && StringUtils.hasText(props.getContentSidOwner())) {
            sendTemplateMessage(props.getOwnerWhatsappTo(), props.getContentSidOwner(), ownerVariables(booking));
        } else {
            log.warn("Owner WhatsApp destination or template not configured — skipping owner notification.");
        }

        if (StringUtils.hasText(booking.getPhone()) && StringUtils.hasText(props.getContentSidCustomer())) {
            sendTemplateMessage(toWhatsAppAddress(booking.getPhone()), props.getContentSidCustomer(), customerVariables(booking));
        } else {
            log.warn("Customer phone or template not configured — skipping customer confirmation.");
        }
    }

    public void sendBookingConfirmation(Booking booking) {
        if (!initialized) return;
        
        if (StringUtils.hasText(booking.getCustomer().getPhone()) && StringUtils.hasText(props.getContentSidCustomerConfirmed())) {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("1", nullSafe(booking.getCustomer().getName()));
            vars.put("2", nullSafe(booking.getPickupDate()) + " " + nullSafe(booking.getPickupTime()));
            vars.put("3", nullSafe(booking.getPickupLocation()));
            vars.put("4", nullSafe(booking.getDriverName())); // Assuming template supports driver name
            sendTemplateMessage(toWhatsAppAddress(booking.getCustomer().getPhone()), props.getContentSidCustomerConfirmed(), vars);
        }
    }

    private void sendTemplateMessage(String to, String contentSid, Map<String, String> variables) {
        try {
            MessageCreator creator = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(props.getWhatsappFrom()),
                    ""
            ).setContentSid(contentSid);
            if (!variables.isEmpty()) {
                creator.setContentVariables(objectMapper.writeValueAsString(variables));
            }
            Message message = creator.create();
            log.info("WhatsApp message sent to {} (sid={})", to, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}", to, e);
        }
    }

    private Map<String, String> ownerVariables(BookingRequest b) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", nullSafe(b.getFullName()));
        vars.put("2", nullSafe(b.getPhone()));
        vars.put("3", nullSafe(b.getPickupLocation()));
        vars.put("4", nullSafe(b.getDropoffLocation()));
        vars.put("5", nullSafe(b.getPickupDate()) + " " + nullSafe(b.getPickupTime()));
        return vars;
    }

    private Map<String, String> customerVariables(BookingRequest b) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", nullSafe(b.getFullName()));
        vars.put("2", nullSafe(b.getPickupDate()) + " " + nullSafe(b.getPickupTime()));
        vars.put("3", nullSafe(b.getPickupLocation()));
        return vars;
    }

    private String toWhatsAppAddress(String rawPhone) {
        String digits = rawPhone.replaceAll("[^+0-9]", "");
        return digits.startsWith("whatsapp:") ? digits : "whatsapp:" + digits;
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}
