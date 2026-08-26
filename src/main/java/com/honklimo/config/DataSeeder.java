package com.honklimo.config;

import com.honklimo.entity.PricingRate;
import com.honklimo.entity.Vehicle;
import com.honklimo.repository.PricingRateRepository;
import com.honklimo.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Seeds the fleet/pricing tables from the site's original static content, but only on an empty
// database — admin edits afterward are never overwritten.
@Component
public class DataSeeder implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final PricingRateRepository pricingRateRepository;

    public DataSeeder(VehicleRepository vehicleRepository, PricingRateRepository pricingRateRepository) {
        this.vehicleRepository = vehicleRepository;
        this.pricingRateRepository = pricingRateRepository;
    }

    @Override
    public void run(String... args) {
        seedVehicles();
        seedPricingRates();
    }

    private void seedVehicles() {
        if (vehicleRepository.count() > 0) return;

        vehicleRepository.saveAll(java.util.List.of(
                vehicle("sedan", "Executive Sedan", 3, 2, 89.0, "bi-car-front-fill",
                        "Seats up to 3 passengers. Free Wi-Fi & bottled water.", 1),
                vehicle("suv", "Luxury SUV", 6, 4, 110.0, "bi-truck-front-fill",
                        "Seats up to 6 passengers. Premium leather interior.", 2),
                vehicle("sprinter", "Sprinter Van", 14, 10, 115.0, "bi-bus-front-fill",
                        "Seats up to 14 passengers. Wi-Fi & USB charging onboard.", 3),
                vehicle("stretch", "Stretch Limo", 10, 6, 120.0, "bi-car-front",
                        "Seats up to 10 passengers. LED ambient lighting & bar.", 4),
                vehicle("partybus", "Party Bus", 30, 15, 250.0, "bi-bus-front",
                        "Seats up to 20-40 passengers. Dance floor & sound system.", 5),
                vehicle("motorcoach", "Mini Bus / Motor Coach", 55, 30, 210.0, "bi-bus-front-fill",
                        "Seats up to 55 passengers. Climate-controlled cabin.", 6)
        ));
    }

    private void seedPricingRates() {
        if (pricingRateRepository.count() > 0) return;

        pricingRateRepository.saveAll(java.util.List.of(
                rate("sedan", "Sedan", 95.0, 3.0, 1),
                rate("suv", "SUV", 110.0, 4.0, 2),
                rate("stretch", "Luxury Sedan (Stretch Limo)", 110.0, 4.5, 3),
                rate("sprinter", "Luxury SUV (Sprinter Van)", 135.0, 6.0, 4)
        ));
    }

    private Vehicle vehicle(String key, String name, int capacity, int luggage, double pricePerHour,
                            String icon, String description, int order) {
        Vehicle v = new Vehicle();
        v.setVehicleKey(key);
        v.setName(name);
        v.setCapacity(capacity);
        v.setLuggage(luggage);
        v.setPricePerHour(pricePerHour);
        v.setIconClass(icon);
        v.setDescription(description);
        v.setDisplayOrder(order);
        return v;
    }

    private PricingRate rate(String key, String label, double flat, double perMile, int order) {
        PricingRate r = new PricingRate();
        r.setVehicleKey(key);
        r.setLabel(label);
        r.setFlatAirportRate(flat);
        r.setPerMileRate(perMile);
        r.setDisplayOrder(order);
        return r;
    }
}
