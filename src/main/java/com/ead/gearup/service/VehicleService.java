package com.ead.gearup.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.dto.vehicle.VehicleUpdateDTO;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.VehicleDTOConverter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final CurrentUserService currentUserService;
    private final VehicleDTOConverter converter;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponseDTO createVehicle(VehicleCreateDTO createVehicleDTO) {

        Customer customer = customerRepository.findById(currentUserService.getCurrentEntityId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found: " + currentUserService.getCurrentEntityId()));

        // Check if vehicle already exists by license plate
        vehicleRepository.findByLicensePlate(createVehicleDTO.getLicensePlate())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Vehicle with this license plate already exists");
                });

        // Check if vehicle already exists by vin
        vehicleRepository.findByVin(createVehicleDTO.getVin())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Vehicle with this VIN already exists");
                });

        Vehicle vehicle = converter.convertToEntity(createVehicleDTO, customer);

        return converter.convertToResponseDto(vehicleRepository.save(vehicle));
    }

    public VehicleResponseDTO getVehicleById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + id));

        return converter.convertToResponseDto(vehicle);
    }

    public List<VehicleResponseDTO> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return vehicles.stream()
                .map(vehicle -> converter.convertToResponseDto(vehicle))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteVehicle(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid vehicle ID");
        }

        if (!vehicleRepository.existsById(id)) {
            throw new VehicleNotFoundException("Vehicle not found with id: " + id);
        }

        vehicleRepository.deleteById(id);
    }

    public VehicleResponseDTO updateVehicle(Long id, VehicleUpdateDTO updateVehicleDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid Vehicle ID");
        }

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + id));

        converter.updateEntityFromDto(existingVehicle, updateVehicleDTO);

        Vehicle savedVehicle = vehicleRepository.save(existingVehicle);

        return converter.convertToResponseDto(savedVehicle);
    }

    public List<VehicleResponseDTO> getVehiclesForCurrentCustomer() {
        Long customerId = currentUserService.getCurrentEntityId();

        List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);
        return vehicles.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }
}
