package com.honklimo.service;

// pricingType: FLAT, PER_MILE, or CALL_FOR_PRICING (fare is null in that case).
public record FareEstimate(String pricingType, Double fare, String note) {

    static FareEstimate flat(double fare, String note) {
        return new FareEstimate("FLAT", round2(fare), note);
    }

    static FareEstimate perMile(double fare, String note) {
        return new FareEstimate("PER_MILE", round2(fare), note);
    }

    static FareEstimate callForPricing(String note) {
        return new FareEstimate("CALL_FOR_PRICING", null, note);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
