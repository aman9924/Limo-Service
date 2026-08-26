package com.chicagoelitelimo.service;

import com.chicagoelitelimo.config.MailProperties;
import com.chicagoelitelimo.entity.Booking;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, MailProperties mailProperties, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.templateEngine = templateEngine;
    }

    private boolean isConfigured() {
        return StringUtils.hasText(mailHost);
    }

    public void sendBookingNotifications(Booking booking) {
        if (!isConfigured()) {
            log.warn("Skipping email notifications — SMTP is not configured.");
            return;
        }

        String customerEmail = booking.getCustomer().getEmail();
        if (StringUtils.hasText(customerEmail)) {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("baseUrl", baseUrl);
            String htmlBody = templateEngine.process("email/customer-acknowledgment", context);
            sendHtmlEmail(customerEmail, "Your HONK Limousine booking request — " + booking.getBookingReference(), htmlBody);
        }

        if (StringUtils.hasText(mailProperties.getOwnerEmail())) {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("baseUrl", baseUrl);
            String htmlBody = templateEngine.process("email/owner-alert", context);
            sendHtmlEmail(mailProperties.getOwnerEmail(), "New Booking Request: " + booking.getBookingReference(), htmlBody);
        }
    }

    public void sendBookingConfirmation(Booking booking) {
        if (!isConfigured()) return;
        String customerEmail = booking.getCustomer().getEmail();
        if (StringUtils.hasText(customerEmail)) {
            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("baseUrl", baseUrl);
            String htmlBody = templateEngine.process("email/customer-confirmation", context);
            sendHtmlEmail(customerEmail, "Your HONK Limousine ride is CONFIRMED — " + booking.getBookingReference(), htmlBody);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml
            mailSender.send(message);
            log.info("HTML Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", to, e);
        }
    }
}
