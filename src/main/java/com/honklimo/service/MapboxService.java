package com.honklimo.service;

import com.honklimo.config.MapboxProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class MapboxService {

    private final MapboxProperties props;
    private final RestTemplate restTemplate;

    public MapboxService(MapboxProperties props, RestTemplateBuilder builder) {
        this.props = props;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(props.getAccessToken());
    }

    // pickup/dropoff coordinates come from the browser's Mapbox autocomplete selection — no server-side geocoding needed.
    @SuppressWarnings("unchecked")
    public RouteResult getDrivingRoute(double pickupLng, double pickupLat, double dropoffLng, double dropoffLat) {
        if (!isConfigured()) {
            throw new IllegalStateException("Mapbox is not configured on the server.");
        }

        String url = String.format(
                "https://api.mapbox.com/directions/v5/mapbox/driving/%f,%f;%f,%f?access_token=%s&overview=simplified",
                pickupLng, pickupLat, dropoffLng, dropoffLat, props.getAccessToken());

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("routes")) {
            throw new IllegalStateException("No route found between those locations.");
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
        if (routes.isEmpty()) {
            throw new IllegalStateException("No route found between those locations.");
        }

        Map<String, Object> firstRoute = routes.get(0);
        double distanceMeters = ((Number) firstRoute.get("distance")).doubleValue();
        double durationSeconds = ((Number) firstRoute.get("duration")).doubleValue();
        String geometry = (String) firstRoute.get("geometry");

        double distanceMiles = distanceMeters / 1609.344;
        double durationMinutes = durationSeconds / 60.0;
        return new RouteResult(distanceMiles, durationMinutes, geometry);
    }

    @SuppressWarnings("unchecked")
    public double[] geocode(String query) {
        if (!isConfigured() || !StringUtils.hasText(query)) {
            return null;
        }
        try {
            String url = String.format("https://api.mapbox.com/geocoding/v5/mapbox.places/%s.json?access_token=%s&limit=1&country=us&types=poi,address,place&autocomplete=true",
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8),
                    props.getAccessToken());
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("features")) {
                List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
                if (!features.isEmpty()) {
                    List<Number> center = (List<Number>) features.get(0).get("center");
                    if (center != null && center.size() >= 2) {
                        return new double[]{center.get(0).doubleValue(), center.get(1).doubleValue()};
                    }
                }
            }
        } catch (Exception e) {
            // ignore and return null to trigger fallback
        }
        return null;
    }
}
