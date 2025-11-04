package com.ead.gearup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVin(String vin);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndVehicleIdNot(String licensePlate, Long vehicleId);

    boolean existsByVinAndVehicleIdNot(String vin, Long vehicleId);

    List<Vehicle> findByCustomer_CustomerId(Long customerId);
}
