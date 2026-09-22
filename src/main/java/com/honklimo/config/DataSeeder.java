package com.honklimo.config;

import com.honklimo.entity.AddonPricing;
import com.honklimo.entity.PricingRate;
import com.honklimo.entity.Vehicle;
import com.honklimo.repository.AddonPricingRepository;
import com.honklimo.repository.PricingRateRepository;
import com.honklimo.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final PricingRateRepository pricingRateRepository;
    private final AddonPricingRepository addonPricingRepository;

    public DataSeeder(VehicleRepository vehicleRepository, PricingRateRepository pricingRateRepository,
                       AddonPricingRepository addonPricingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.pricingRateRepository = pricingRateRepository;
        this.addonPricingRepository = addonPricingRepository;
    }

    @Override
    public void run(String... args) {
        seedVehicles();
        seedPricingRates();
        seedAddonPricing();
    }

    private void seedVehicles() {
        vehicleRepository.deleteAll(); 
        
        vehicleRepository.saveAll(java.util.List.of(
                vehicle("regular_sedan", "Regular Sedan", 3, 3, 89.0, "bi-car-front-fill", "/images/fleet/New Cars Images/roy.jpg", "Seats up to 3 passengers. Comfort and style.", 1),
                vehicle("luxury_sedan", "Luxury Sedan", 3, 3, 120.0, "bi-car-front-fill", "/images/fleet/New Cars Images/Mercides.jpg", "Seats up to 3 passengers. Ultimate luxury experience.", 2),
                vehicle("regular_suv", "Regular SUV", 6, 6, 110.0, "bi-truck-front-fill", "/images/fleet/New Cars Images/Black car.jpg", "Seats up to 6 passengers. Perfect for families.", 3),
                vehicle("luxury_suv", "Luxury SUV", 6, 6, 140.0, "bi-truck-front-fill", "/images/fleet/New Cars Images/Bulletproof Cadillac Escalade.jpg", "Seats up to 6 passengers. Premium cabin.", 4),
                vehicle("premium_cars", "Premium / Luxury Cars", 4, 3, 150.0, "bi-star-fill", "", "High-end luxury vehicles and executive cars.", 5),
                vehicle("party_buses", "Party Buses", 20, 10, 200.0, "bi-bus-front", "", "Perfect for large groups and celebrations.", 6),
                vehicle("charter", "Charter Vehicles", 50, 50, 300.0, "bi-bus-front-fill", "", "Large scale transportation for events.", 7),
                vehicle("wedding", "Wedding Transportation", 4, 2, 250.0, "bi-heart-fill", "/images/fleet/New Cars Images/limo.jpg", "Elegant vehicles for your special day.", 8),
                vehicle("specialty", "Other Specialty Vehicles", 10, 5, 200.0, "bi-gem", "", "Unique transportation options tailored to you.", 9)
        ));
    }

    private void seedPricingRates() {
        pricingRateRepository.deleteAll(); 
        
        pricingRateRepository.saveAll(java.util.List.of(
                rate("regular_sedan", "Regular Sedan", 85.0, 114.0, 135.0, 4.0, false, 1),
                rate("luxury_sedan", "Luxury Sedan", 100.0, 129.0, 150.0, 5.0, false, 2),
                rate("regular_suv", "Regular SUV", 115.0, 144.0, 165.0, 6.0, false, 3),
                rate("luxury_suv", "Luxury SUV", 135.0, 164.0, 185.0, 7.0, false, 4),
                rate("premium_cars", "Premium / Luxury Cars", 0.0, 0.0, 0.0, 0.0, true, 5),
                rate("party_buses", "Party Buses", 0.0, 0.0, 0.0, 0.0, true, 6),
                rate("charter", "Charter Vehicles", 0.0, 0.0, 0.0, 0.0, true, 7),
                rate("wedding", "Wedding Transportation", 0.0, 0.0, 0.0, 0.0, true, 8),
                rate("specialty", "Other Specialty Vehicles", 0.0, 0.0, 0.0, 0.0, true, 9)
        ));
    }

    private void seedAddonPricing() {
        if (addonPricingRepository.count() > 0) return;
        addonPricingRepository.save(new AddonPricing());
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

    private PricingRate rate(String key, String label, double t1, double t2, double t3, double perMile, boolean callForPricing, int order) {
        PricingRate r = new PricingRate();
        r.setVehicleKey(key);
        r.setLabel(label);
        r.setTier1Price(t1);
        r.setTier2Price(t2);
        r.setTier3Price(t3);
        r.setPerMileRate(perMile);
        r.setCallForPricingOnly(callForPricing);
        r.setDisplayOrder(order);
        return r;
    }
}
