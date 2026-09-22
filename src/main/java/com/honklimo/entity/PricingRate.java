package com.honklimo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pricing_rates")
public class PricingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vehicleKey;

    private String label;
    
    private Double tier1Price; // 1-10 miles
    
    private Double tier2Price; // 11-20 miles
    
    private Double tier3Price; // 21-30 miles

    private Double perMileRate; // Over 30 miles

    private Boolean callForPricingOnly = false;

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

    public Double getTier1Price() {
        return tier1Price;
    }

    public void setTier1Price(Double tier1Price) {
        this.tier1Price = tier1Price;
    }

    public Double getTier2Price() {
        return tier2Price;
    }

    public void setTier2Price(Double tier2Price) {
        this.tier2Price = tier2Price;
    }

    public Double getTier3Price() {
        return tier3Price;
    }

    public void setTier3Price(Double tier3Price) {
        this.tier3Price = tier3Price;
    }

    public Double getPerMileRate() {
        return perMileRate;
    }

    public void setPerMileRate(Double perMileRate) {
        this.perMileRate = perMileRate;
    }

    public Boolean getCallForPricingOnly() {
        return callForPricingOnly;
    }

    public void setCallForPricingOnly(Boolean callForPricingOnly) {
        this.callForPricingOnly = callForPricingOnly;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
