package com.honklimo.service;

import com.honklimo.entity.PricingRate;
import com.honklimo.repository.PricingRateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PricingService {

    private final PricingRateRepository pricingRateRepository;

    public PricingService(PricingRateRepository pricingRateRepository) {
        this.pricingRateRepository = pricingRateRepository;
    }

    public FareEstimate estimate(String pickupLocation, String dropoffLocation, double distanceMiles, String vehicleType) {
        Optional<PricingRate> rate = pricingRateRepository.findByVehicleKey(
                vehicleType == null ? "" : vehicleType.toLowerCase(Locale.ROOT));
        
        if (rate.isEmpty()) {
            return FareEstimate.callForPricing("Contact us for availability and customized pricing.");
        }

        PricingRate r = rate.get();

        if (Boolean.TRUE.equals(r.getCallForPricingOnly())) {
            return FareEstimate.callForPricing("Contact us for availability and customized pricing.");
        }

        if (distanceMiles > 50.0) {
            return FareEstimate.callForPricing("Long-distance trip (Over 50 Miles). Please contact us for a custom quote.");
        }

        if (distanceMiles <= 10.0) {
            return FareEstimate.flat(r.getTier1Price(), "Tier 1: 1-10 Miles");
        } else if (distanceMiles <= 20.0) {
            return FareEstimate.flat(r.getTier2Price(), "Tier 2: 11-20 Miles");
        } else if (distanceMiles <= 30.0) {
            return FareEstimate.flat(r.getTier3Price(), "Tier 3: 21-30 Miles");
        } else {
            // 31 - 50 miles
            double extraMiles = Math.max(0, distanceMiles - 30.0);
            double price = r.getTier3Price() + (extraMiles * r.getPerMileRate());
            return FareEstimate.flat(price, String.format(Locale.ROOT, "Tier 3 + %.1f extra miles @ $%.2f/mi", extraMiles, r.getPerMileRate()));
        }
    }
}
