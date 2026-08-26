package com.honklimo.controller;

import com.honklimo.dto.EstimateRequest;
import com.honklimo.service.FareEstimate;
import com.honklimo.service.MapboxService;
import com.honklimo.service.PricingService;
import com.honklimo.service.RouteResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EstimateController {

    private static final Logger log = LoggerFactory.getLogger(EstimateController.class);

    private final MapboxService mapboxService;
    private final PricingService pricingService;

    public EstimateController(MapboxService mapboxService, PricingService pricingService) {
        this.mapboxService = mapboxService;
        this.pricingService = pricingService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimate(@Valid @RequestBody EstimateRequest request) {
        try {
            Double pickupLng = request.getPickupLng();
            Double pickupLat = request.getPickupLat();
            if (pickupLng == null || pickupLat == null) {
                log.info("Backend geocoding pickup: {}", request.getPickupLocation());
                double[] coords = mapboxService.geocode(request.getPickupLocation());
                if (coords == null) {
                    log.warn("Backend geocoding failed for pickup");
                    return fallbackCallForPricing();
                }
                pickupLng = coords[0];
                pickupLat = coords[1];
                log.info("Pickup geocoded to: lng={}, lat={}", pickupLng, pickupLat);
            }

            Double dropoffLng = request.getDropoffLng();
            Double dropoffLat = request.getDropoffLat();
            if (dropoffLng == null || dropoffLat == null) {
                log.info("Backend geocoding dropoff: {}", request.getDropoffLocation());
                double[] coords = mapboxService.geocode(request.getDropoffLocation());
                if (coords == null) {
                    log.warn("Backend geocoding failed for dropoff");
                    return fallbackCallForPricing();
                }
                dropoffLng = coords[0];
                dropoffLat = coords[1];
                log.info("Dropoff geocoded to: lng={}, lat={}", dropoffLng, dropoffLat);
            }

            log.info("Routing from [{},{}] to [{},{}]", pickupLng, pickupLat, dropoffLng, dropoffLat);

            RouteResult route;
            try {
                route = mapboxService.getDrivingRoute(
                        pickupLng, pickupLat,
                        dropoffLng, dropoffLat);
            } catch (Exception e) {
                log.warn("Mapbox routing failed: {}", e.getMessage());
                return fallbackCallForPricing();
            }

            FareEstimate fare = pricingService.estimate(
                    request.getPickupLocation(), request.getDropoffLocation(),
                    route.distanceMiles(), request.getVehicleType());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "success");
            body.put("distanceMiles", Math.round(route.distanceMiles() * 10.0) / 10.0);
            body.put("durationMinutes", Math.round(route.durationMinutes()));
            body.put("pricingType", fare.pricingType());
            body.put("estimatedFare", fare.fare());
            body.put("note", fare.note());
            body.put("geometry", route.geometry());
            return ResponseEntity.ok(body);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to calculate distance/fare estimate", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "error",
                    "message", "Could not calculate an estimate right now. Please call us for a quote."
            ));
        }
    }

    private ResponseEntity<Map<String, Object>> fallbackCallForPricing() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("distanceMiles", 0.0);
        body.put("durationMinutes", 0);
        body.put("pricingType", "CALL_FOR_PRICING");
        body.put("estimatedFare", null);
        body.put("note", "Custom location entered. Final price will be quoted by our team.");
        body.put("geometry", null);
        return ResponseEntity.ok(body);
    }
}
