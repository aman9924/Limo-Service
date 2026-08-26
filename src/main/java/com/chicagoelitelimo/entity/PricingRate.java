package com.chicagoelitelimo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Mirrors the pricing tiers used by PricingService: flat airport rate + per-mile rate per vehicle.
@Entity
@Table(name = "pricing_rates")
public class PricingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vehicleKey;

    private String label;
    private Double flatAirportRate;
    private Double perMileRate;
    private Integer displayOrder = 0;

    public Long getId() {
        return id;
    }

    public String getVehicleKey() {
        return vehicleKey;
    }

    public void setVehicleKey(String vehicleKey) {
        this.vehicleKey = vehicleKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getFlatAirportRate() {
        return flatAirportRate;
    }

    public void setFlatAirportRate(Double flatAirportRate) {
        this.flatAirportRate = flatAirportRate;
    }

    public Double getPerMileRate() {
        return perMileRate;
    }

    public void setPerMileRate(Double perMileRate) {
        this.perMileRate = perMileRate;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
