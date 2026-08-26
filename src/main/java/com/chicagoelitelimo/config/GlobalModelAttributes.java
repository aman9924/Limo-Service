package com.chicagoelitelimo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Injects site-wide values (base URL for canonical/OG tags, optional GA tracking ID) into every
// public page's model, so each template doesn't need its controller method to set them manually.
@ControllerAdvice(basePackages = "com.chicagoelitelimo.controller")
public class GlobalModelAttributes {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${analytics.google-tracking-id:}")
    private String gaTrackingId;

    @ModelAttribute("baseUrl")
    public String baseUrl() {
        return baseUrl;
    }

    @ModelAttribute("gaTrackingId")
    public String gaTrackingId() {
        return gaTrackingId;
    }
}
