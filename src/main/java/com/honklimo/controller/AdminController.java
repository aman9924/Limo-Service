package com.honklimo.controller;

import com.honklimo.entity.Booking;
import com.honklimo.entity.BookingStatus;
import com.honklimo.entity.PricingRate;
import com.honklimo.entity.Vehicle;
import com.honklimo.entity.AddonPricing;
import com.honklimo.repository.PricingRateRepository;
import com.honklimo.repository.VehicleRepository;
import com.honklimo.repository.AddonPricingRepository;
import com.honklimo.service.BookingService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingService bookingService;
    private final VehicleRepository vehicleRepository;
    private final PricingRateRepository pricingRateRepository;
    private final AddonPricingRepository addonPricingRepository;

    public AdminController(BookingService bookingService, VehicleRepository vehicleRepository,
                            PricingRateRepository pricingRateRepository, AddonPricingRepository addonPricingRepository) {
        this.bookingService = bookingService;
        this.vehicleRepository = vehicleRepository;
        this.pricingRateRepository = pricingRateRepository;
        this.addonPricingRepository = addonPricingRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/bookings")
    public String bookings(@RequestParam(name = "q", required = false) String q, Model model) {
        List<Booking> bookings = bookingService.searchForAdmin(q);
        model.addAttribute("bookings", bookings);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/dashboard";
    }

    @PostMapping("/bookings/{id}/update")
    public String updateBooking(@PathVariable Long id, @RequestParam("status") BookingStatus status,
                                @RequestParam(name = "driverName", required = false) String driverName,
                                @RequestParam(name = "driverPhone", required = false) String driverPhone,
                                @RequestParam(name = "q", required = false) String q) {
        bookingService.updateBooking(id, status, driverName, driverPhone);
        return redirectToBookings(q);
    }

    @PostMapping("/bookings/{id}/delete")
    public String deleteBooking(@PathVariable Long id, @RequestParam(name = "q", required = false) String q) {
        bookingService.deleteBooking(id);
        return redirectToBookings(q);
    }

    @GetMapping("/bookings/export")
    public void exportCsv(@RequestParam(name = "q", required = false) String q, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"bookings.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Reference,Customer Name,Phone,Email,Service Type,Vehicle,Pickup,Drop-off,Date,Time,Status");
            for (Booking b : bookingService.searchForAdmin(q)) {
                writer.println(String.join(",",
                        csv(b.getBookingReference()),
                        csv(b.getCustomer().getName()),
                        csv(b.getCustomer().getPhone()),
                        csv(b.getCustomer().getEmail()),
                        csv(b.getServiceType()),
                        csv(b.getVehicleType()),
                        csv(b.getPickupLocation()),
                        csv(b.getDropoffLocation()),
                        csv(b.getPickupDate()),
                        csv(b.getPickupTime()),
                        csv(b.getStatus().name())
                ));
            }
        }
    }

    private String redirectToBookings(String q) {
        return q == null || q.isBlank() ? "redirect:/admin/bookings" : "redirect:/admin/bookings?q=" + q;
    }

    // ---- Fleet management ----

    @GetMapping("/fleet")
    public String fleet(Model model) {
        model.addAttribute("vehicles", vehicleRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("newVehicle", new Vehicle());
        return "admin/fleet";
    }

    @PostMapping("/fleet")
    public String createVehicle(Vehicle vehicle) {
        vehicleRepository.save(vehicle);
        return "redirect:/admin/fleet";
    }

    @PostMapping("/fleet/{id}")
    public String updateVehicle(@PathVariable Long id, Vehicle vehicle) {
        vehicle.setId(id);
        vehicleRepository.save(vehicle);
        return "redirect:/admin/fleet";
    }

    @PostMapping("/fleet/{id}/delete")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleRepository.deleteById(id);
        return "redirect:/admin/fleet";
    }

    // ---- Pricing management ----

    @GetMapping("/pricing")
    public String pricing(Model model) {
        model.addAttribute("rates", pricingRateRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("addonPricing", addonPricingRepository.findById(1L).orElseGet(AddonPricing::new));
        return "admin/pricing";
    }

    @PostMapping("/pricing/addons")
    public String updateAddonPricing(@RequestParam Double meetAndGreetFee) {
        AddonPricing addon = addonPricingRepository.findById(1L).orElseGet(AddonPricing::new);
        addon.setMeetAndGreetFee(meetAndGreetFee);
        addonPricingRepository.save(addon);
        return "redirect:/admin/pricing";
    }

    @PostMapping("/pricing/{id}")
    public String updatePricing(@PathVariable Long id, @RequestParam String label,
                                 @RequestParam(defaultValue = "0") Double tier1Price, 
                                 @RequestParam(defaultValue = "0") Double tier2Price, 
                                 @RequestParam(defaultValue = "0") Double tier3Price, 
                                 @RequestParam(defaultValue = "0") Double perMileRate,
                                 @RequestParam(required = false) Boolean callForPricingOnly) {
        PricingRate rate = pricingRateRepository.findById(id).orElseThrow();
        rate.setLabel(label);
        rate.setTier1Price(tier1Price);
        rate.setTier2Price(tier2Price);
        rate.setTier3Price(tier3Price);
        rate.setPerMileRate(perMileRate);
        rate.setCallForPricingOnly(callForPricingOnly != null && callForPricingOnly);
        pricingRateRepository.save(rate);
        return "redirect:/admin/pricing";
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
