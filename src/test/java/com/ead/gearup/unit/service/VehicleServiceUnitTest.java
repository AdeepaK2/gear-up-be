package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.dto.vehicle.VehicleUpdateDTO;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.VehicleService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.VehicleDTOConverter;

@ExtendWith(MockitoExtension.class)
class VehicleServiceUnitTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private VehicleDTOConverter converter;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle testVehicle;
    private Customer testCustomer;
    private VehicleCreateDTO createDTO;
    private VehicleUpdateDTO updateDTO;
    private VehicleResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);

        // Setup test vehicle
        testVehicle = new Vehicle();
        testVehicle.setVehicleId(1L);
        testVehicle.setMake("Toyota");
        testVehicle.setModel("Camry");
        testVehicle.setYear(2020);
        testVehicle.setLicensePlate("ABC123");
        testVehicle.setVin("VIN123456");
        testVehicle.setCustomer(testCustomer);

        // Setup DTOs
        createDTO = new VehicleCreateDTO();
        createDTO.setMake("Toyota");
        createDTO.setModel("Camry");
        createDTO.setYear(2020);
        createDTO.setLicensePlate("ABC123");
        createDTO.setVin("VIN123456");

        updateDTO = new VehicleUpdateDTO();
        updateDTO.setMake("Honda");
        updateDTO.setModel("Accord");

        responseDTO = new VehicleResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setMake("Toyota");
        responseDTO.setModel("Camry");
    }

    // ========== createVehicle() Tests ==========
    @Test
    void testCreateVehicle_Success() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(vehicleRepository.findByLicensePlate(anyString())).thenReturn(Optional.empty());
        when(vehicleRepository.findByVin(anyString())).thenReturn(Optional.empty());
        when(converter.convertToEntity(any(VehicleCreateDTO.class), any(Customer.class))).thenReturn(testVehicle);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);
        when(converter.convertToResponseDto(any(Vehicle.class))).thenReturn(responseDTO);

        // Act
        VehicleResponseDTO result = vehicleService.createVehicle(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Toyota", result.getMake());
        verify(customerRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void testCreateVehicle_CustomerNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> vehicleService.createVehicle(createDTO));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void testCreateVehicle_DuplicateLicensePlate() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(vehicleRepository.findByLicensePlate("ABC123")).thenReturn(Optional.of(testVehicle));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.createVehicle(createDTO));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void testCreateVehicle_DuplicateVIN() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(vehicleRepository.findByLicensePlate(anyString())).thenReturn(Optional.empty());
        when(vehicleRepository.findByVin("VIN123456")).thenReturn(Optional.of(testVehicle));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.createVehicle(createDTO));
        verify(vehicleRepository, never()).save(any());
    }

    // ========== getVehicleById() Tests ==========
    @Test
    void testGetVehicleById_Success() {
        // Arrange
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(converter.convertToResponseDto(testVehicle)).thenReturn(responseDTO);

        // Act
        VehicleResponseDTO result = vehicleService.getVehicleById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(vehicleRepository, times(1)).findById(1L);
    }

    @Test
    void testGetVehicleById_NotFound() {
        // Arrange
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VehicleNotFoundException.class, () -> vehicleService.getVehicleById(999L));
    }

    @Test
    void testGetVehicleById_InvalidId_Null() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.getVehicleById(null));
        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void testGetVehicleById_InvalidId_Zero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.getVehicleById(0L));
        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void testGetVehicleById_InvalidId_Negative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.getVehicleById(-1L));
        verify(vehicleRepository, never()).findById(any());
    }

    // ========== getAllVehicles() Tests ==========
    @Test
    void testGetAllVehicles_Success() {
        // Arrange
        List<Vehicle> vehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findAll()).thenReturn(vehicles);
        when(converter.convertToResponseDto(any(Vehicle.class))).thenReturn(responseDTO);

        // Act
        List<VehicleResponseDTO> result = vehicleService.getAllVehicles();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void testGetAllVehicles_EmptyList() {
        // Arrange
        when(vehicleRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<VehicleResponseDTO> result = vehicleService.getAllVehicles();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== deleteVehicle() Tests ==========
    @Test
    void testDeleteVehicle_Success() {
        // Arrange
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(vehicleRepository).deleteById(1L);

        // Act
        vehicleService.deleteVehicle(1L);

        // Assert
        verify(vehicleRepository, times(1)).existsById(1L);
        verify(vehicleRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteVehicle_NotFound() {
        // Arrange
        when(vehicleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(VehicleNotFoundException.class, () -> vehicleService.deleteVehicle(999L));
        verify(vehicleRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteVehicle_InvalidId_Null() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.deleteVehicle(null));
        verify(vehicleRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteVehicle_InvalidId_Negative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.deleteVehicle(-1L));
        verify(vehicleRepository, never()).deleteById(any());
    }

    // ========== updateVehicle() Tests ==========
    @Test
    void testUpdateVehicle_Success() {
        // Arrange
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        doNothing().when(converter).updateEntityFromDto(any(Vehicle.class), any(VehicleUpdateDTO.class));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);
        when(converter.convertToResponseDto(any(Vehicle.class))).thenReturn(responseDTO);

        // Act
        VehicleResponseDTO result = vehicleService.updateVehicle(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(vehicleRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(testVehicle);
        verify(converter, times(1)).updateEntityFromDto(testVehicle, updateDTO);
    }

    @Test
    void testUpdateVehicle_NotFound() {
        // Arrange
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VehicleNotFoundException.class, () -> vehicleService.updateVehicle(999L, updateDTO));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void testUpdateVehicle_InvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vehicleService.updateVehicle(null, updateDTO));
        assertThrows(IllegalArgumentException.class, () -> vehicleService.updateVehicle(0L, updateDTO));
        verify(vehicleRepository, never()).save(any());
    }

    // ========== getVehiclesForCurrentCustomer() Tests ==========
    @Test
    void testGetVehiclesForCurrentCustomer_Success() {
        // Arrange
        List<Vehicle> vehicles = Arrays.asList(testVehicle);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(vehicleRepository.findByCustomer_CustomerId(1L)).thenReturn(vehicles);
        when(converter.convertToResponseDto(any(Vehicle.class))).thenReturn(responseDTO);

        // Act
        List<VehicleResponseDTO> result = vehicleService.getVehiclesForCurrentCustomer();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(currentUserService, times(1)).getCurrentEntityId();
        verify(vehicleRepository, times(1)).findByCustomer_CustomerId(1L);
    }

    @Test
    void testGetVehiclesForCurrentCustomer_EmptyList() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(vehicleRepository.findByCustomer_CustomerId(1L)).thenReturn(Arrays.asList());

        // Act
        List<VehicleResponseDTO> result = vehicleService.getVehiclesForCurrentCustomer();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
