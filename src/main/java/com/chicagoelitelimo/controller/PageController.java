package com.chicagoelitelimo.controller;

import com.chicagoelitelimo.repository.PricingRateRepository;
import com.chicagoelitelimo.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {

    private final VehicleRepository vehicleRepository;
    private final PricingRateRepository pricingRateRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String[] SITEMAP_PATHS = {
            "/", "/booking", "/fleet", "/services", "/pricing", "/faq", "/about", "/contact", "/track"
    };

    public PageController(VehicleRepository vehicleRepository, PricingRateRepository pricingRateRepository) {
        this.vehicleRepository = vehicleRepository;
        this.pricingRateRepository = pricingRateRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/booking")
    public String booking() {
        return "booking";
    }

    @GetMapping("/fleet")
    public String fleet(Model model) {
        model.addAttribute("vehicles", vehicleRepository.findAllByOrderByDisplayOrderAsc());
        return "fleet";
    }

    @GetMapping("/pricing")
    public String pricing(Model model) {
        model.addAttribute("rates", pricingRateRepository.findAllByOrderByDisplayOrderAsc());
        return "pricing";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/services")
    public String services() {
        return "services";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/track")
    public String track() {
        return "track";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : SITEMAP_PATHS) {
            xml.append("  <url><loc>").append(baseUrl).append(path).append("</loc></url>\n");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }
}
