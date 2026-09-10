package com.honklimo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Single-row settings table for optional add-on services (currently just Meet & Greet).
@Entity
@Table(name = "addon_pricing")
public class AddonPricing {

    @Id
    private Long id = 1L;

    private Double meetAndGreetFee = 25.0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getMeetAndGreetFee() {
        return meetAndGreetFee;
    }

    public void setMeetAndGreetFee(Double meetAndGreetFee) {
        this.meetAndGreetFee = meetAndGreetFee;
    }
}
