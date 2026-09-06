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
        if (vehicleRepository.count() > 0) {
            // Force update all vehicles to the new luxury lineup
            vehicleRepository.findAll().forEach(v -> {
                if ("sedan".equals(v.getVehicleKey())) {
                    v.setName("Executive Sedan");
                    v.setImageUrl("/images/fleet/mercedes-sedan.jpg");
                }
                else if ("suv".equals(v.getVehicleKey())) {
                    v.setName("Luxury SUV");
                    v.setImageUrl("/images/fleet/range-rover.jpg");
                }
                else if ("sprinter".equals(v.getVehicleKey())) {
                    v.setName("Ultra-Luxury Sedan");
                    v.setCapacity(4);
                    v.setLuggage(3);
                    v.setPricePerHour(150.0);
                    v.setImageUrl("/images/fleet/rolls-royce.jpg");
                    v.setDescription("Seats up to 4 passengers. The pinnacle of luxury and comfort.");
                }
                else if ("stretch".equals(v.getVehicleKey())) {
                    v.setName("Stretch Limo");
                    v.setImageUrl("/images/fleet/lincoln.jpg");
                }
                else if ("partybus".equals(v.getVehicleKey())) {
                    v.setName("Premium SUV");
                    v.setCapacity(7);
                    v.setLuggage(6);
                    v.setPricePerHour(125.0);
                    v.setImageUrl("/images/fleet/audi-suv.jpg");
                    v.setDescription("Seats up to 7 passengers. Spacious and sophisticated.");
                }
                else if ("motorcoach".equals(v.getVehicleKey())) {
                    v.setName("Sport Luxury Sedan");
                    v.setCapacity(4);
                    v.setLuggage(3);
                    v.setPricePerHour(130.0);
                    v.setImageUrl("/images/fleet/jaguar.jpg");
                    v.setDescription("Seats up to 4 passengers. Thrilling performance meets elegance.");
                }
                vehicleRepository.save(v);
            });
            return;
        }

        vehicleRepository.saveAll(java.util.List.of(
                vehicle("sedan", "Executive Sedan", 3, 3, 89.0, "bi-car-front-fill", "/images/fleet/mercedes-sedan.jpg",
                        "Seats up to 3 passengers. Free Wi-Fi & bottled water.", 1),
                vehicle("suv", "Luxury SUV", 6, 6, 110.0, "bi-truck-front-fill", "/images/fleet/range-rover.jpg",
                        "Seats up to 6 passengers. Premium leather interior.", 2),
                vehicle("sprinter", "Ultra-Luxury Sedan", 4, 3, 150.0, "bi-car-front-fill", "/images/fleet/rolls-royce.jpg",
                        "Seats up to 4 passengers. The pinnacle of luxury and comfort.", 3),
                vehicle("stretch", "Stretch Limo", 10, 4, 120.0, "bi-car-front", "/images/fleet/lincoln.jpg",
                        "Seats up to 10 passengers. LED ambient lighting & bar.", 4),
                vehicle("partybus", "Premium SUV", 7, 6, 125.0, "bi-truck-front-fill", "/images/fleet/audi-suv.jpg",
                        "Seats up to 7 passengers. Spacious and sophisticated.", 5),
                vehicle("motorcoach", "Sport Luxury Sedan", 4, 3, 130.0, "bi-car-front-fill", "/images/fleet/jaguar.jpg",
                        "Seats up to 4 passengers. Thrilling performance meets elegance.", 6)
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
                            String icon, String imageUrl, String description, int order) {
        Vehicle v = new Vehicle();
        v.setVehicleKey(key);
        v.setName(name);
        v.setCapacity(capacity);
        v.setLuggage(luggage);
        v.setPricePerHour(pricePerHour);
        v.setIconClass(icon);
        v.setImageUrl(imageUrl);
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
