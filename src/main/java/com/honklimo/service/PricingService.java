package com.honklimo.service;

import com.honklimo.entity.PricingRate;
import com.honklimo.repository.PricingRateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Rates come from the pricing_rates table (admin-editable) rather than being hardcoded.
@Service
public class PricingService {

    private final PricingRateRepository pricingRateRepository;

    public PricingService(PricingRateRepository pricingRateRepository) {
        this.pricingRateRepository = pricingRateRepository;
    }

    private static final double AIRPORT_FLAT_RATE_MAX_MILES = 20.0;

    private static final List<String> AIRPORT_KEYWORDS =
            List.of("airport", "o'hare", "ohare", "midway", "ord", "mdw");

    public FareEstimate estimate(String pickupLocation, String dropoffLocation, double distanceMiles, String vehicleType) {
        Optional<PricingRate> rate = pricingRateRepository.findByVehicleKey(
                vehicleType == null ? "" : vehicleType.toLowerCase(Locale.ROOT));
        if (rate.isEmpty()) {
            return FareEstimate.callForPricing("This vehicle is quoted personally by our team.");
        }

        boolean airportTrip = containsAirportKeyword(pickupLocation) || containsAirportKeyword(dropoffLocation);
        PricingRate r = rate.get();

        if (airportTrip && distanceMiles <= AIRPORT_FLAT_RATE_MAX_MILES) {
            return FareEstimate.flat(r.getFlatAirportRate(), "Airport flat rate");
        }
        return FareEstimate.perMile(distanceMiles * r.getPerMileRate(),
                String.format(Locale.ROOT, "%.1f mi x $%.2f/mi", distanceMiles, r.getPerMileRate()));
    }

    private boolean containsAirportKeyword(String location) {
        if (location == null) {
            return false;
        }
        String lower = location.toLowerCase(Locale.ROOT);
        return AIRPORT_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
