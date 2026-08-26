package com.chicagoelitelimo.repository;

import com.chicagoelitelimo.entity.PricingRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRateRepository extends JpaRepository<PricingRate, Long> {
    List<PricingRate> findAllByOrderByDisplayOrderAsc();

    Optional<PricingRate> findByVehicleKey(String vehicleKey);
}
