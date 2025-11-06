package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ead.gearup.dto.vehicle.VehicleCreateDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.dto.vehicle.VehicleUpdateDTO;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class VehicleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @Autowired
    private ObjectMapper objectMapper;

    private VehicleResponseDTO testVehicleResponse;
    private VehicleCreateDTO testVehicleCreate;
    private VehicleUpdateDTO testVehicleUpdate;

    @BeforeEach
    void setUp() {
        testVehicleResponse = new VehicleResponseDTO();
        testVehicleResponse.setId(1L);
        testVehicleResponse.setMake("Toyota");
        testVehicleResponse.setModel("Camry");
        testVehicleResponse.setYear(2020);
        testVehicleResponse.setLicensePlate("ABC123");
        testVehicleResponse.setVin("VIN123456789");

        testVehicleCreate = new VehicleCreateDTO();
        testVehicleCreate.setMake("Toyota");
        testVehicleCreate.setModel("Camry");
        testVehicleCreate.setYear(2020);
        testVehicleCreate.setLicensePlate("ABC123");
        testVehicleCreate.setVin("VIN123456789");

        testVehicleUpdate = new VehicleUpdateDTO();
        testVehicleUpdate.setMake("Honda");
        testVehicleUpdate.setModel("Accord");
    }

    // ========== POST /api/v1/vehicles ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateVehicle_Success() throws Exception {
        // Arrange
        when(vehicleService.createVehicle(any(VehicleCreateDTO.class)))
                .thenReturn(testVehicleResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVehicleCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Vehicle created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.make").value("Toyota"))
                .andExpect(jsonPath("$.data.model").value("Camry"));

        verify(vehicleService, times(1)).createVehicle(any(VehicleCreateDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateVehicle_DuplicateLicensePlate() throws Exception {
        // Arrange
        when(vehicleService.createVehicle(any(VehicleCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("Vehicle with this license plate already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVehicleCreate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateVehicle_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVehicleCreate)))
                .andExpect(status().isUnauthorized());

        verify(vehicleService, never()).createVehicle(any());
    }

    // ========== GET /api/v1/vehicles/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetVehicleById_Success() throws Exception {
        // Arrange
        when(vehicleService.getVehicleById(1L)).thenReturn(testVehicleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.make").value("Toyota"));

        verify(vehicleService, times(1)).getVehicleById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetVehicleById_NotFound() throws Exception {
        // Arrange
        when(vehicleService.getVehicleById(999L))
                .thenThrow(new VehicleNotFoundException("Vehicle not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/vehicles/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/vehicles ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllVehicles_Success() throws Exception {
        // Arrange
        List<VehicleResponseDTO> vehicles = Arrays.asList(testVehicleResponse);
        when(vehicleService.getAllVehicles()).thenReturn(vehicles);

        // Act & Assert
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(vehicleService, times(1)).getAllVehicles();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllVehicles_EmptyList() throws Exception {
        // Arrange
        when(vehicleService.getAllVehicles()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== GET /api/v1/vehicles/customer/me ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetVehiclesForCurrentCustomer_Success() throws Exception {
        // Arrange
        List<VehicleResponseDTO> vehicles = Arrays.asList(testVehicleResponse);
        when(vehicleService.getVehiclesForCurrentCustomer()).thenReturn(vehicles);

        // Act & Assert
        mockMvc.perform(get("/api/v1/vehicles/customer/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].make").value("Toyota"));

        verify(vehicleService, times(1)).getVehiclesForCurrentCustomer();
    }

    // ========== PATCH /api/v1/vehicles/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testUpdateVehicle_Success() throws Exception {
        // Arrange
        VehicleResponseDTO updatedResponse = new VehicleResponseDTO();
        updatedResponse.setId(1L);
        updatedResponse.setMake("Honda");
        updatedResponse.setModel("Accord");

        when(vehicleService.updateVehicle(anyLong(), any(VehicleUpdateDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/vehicles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVehicleUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.make").value("Honda"));

        verify(vehicleService, times(1)).updateVehicle(anyLong(), any(VehicleUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testUpdateVehicle_NotFound() throws Exception {
        // Arrange
        when(vehicleService.updateVehicle(anyLong(), any(VehicleUpdateDTO.class)))
                .thenThrow(new VehicleNotFoundException("Vehicle not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/vehicles/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testVehicleUpdate)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /api/v1/vehicles/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeleteVehicle_Success() throws Exception {
        // Arrange
        doNothing().when(vehicleService).deleteVehicle(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Vehicle deleted successfully"));

        verify(vehicleService, times(1)).deleteVehicle(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeleteVehicle_NotFound() throws Exception {
        // Arrange
        doThrow(new VehicleNotFoundException("Vehicle not found"))
                .when(vehicleService).deleteVehicle(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/vehicles/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteVehicle_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/vehicles/1"))
                .andExpect(status().isUnauthorized());

        verify(vehicleService, never()).deleteVehicle(anyLong());
    }
}
