package com.honklimo.service;

// Result of a Mapbox Directions API driving route lookup.
public record RouteResult(double distanceMiles, double durationMinutes, String geometry) {
}
