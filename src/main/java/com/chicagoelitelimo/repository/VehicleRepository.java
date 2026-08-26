package com.chicagoelitelimo.repository;

import com.chicagoelitelimo.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOrderByDisplayOrderAsc();
}
