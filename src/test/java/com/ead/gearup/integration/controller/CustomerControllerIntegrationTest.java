package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.dto.customer.CustomerUpdateDTO;
import com.ead.gearup.dto.customer.CustomerDashboardDTO;
import com.ead.gearup.dto.customer.CustomerHeaderDTO;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerResponseDTO testCustomerResponse;
    private CustomerRequestDTO testCustomerRequest;
    private CustomerUpdateDTO testCustomerUpdate;

    @BeforeEach
    void setUp() {
        testCustomerResponse = new CustomerResponseDTO();
        testCustomerResponse.setCustomerId(1L);
        testCustomerResponse.setName("Test Customer");
        testCustomerResponse.setEmail("test@example.com");
        testCustomerResponse.setPhoneNumber("1234567890");

        testCustomerRequest = new CustomerRequestDTO();
        testCustomerRequest.setPhoneNumber("1234567890");

        testCustomerUpdate = new CustomerUpdateDTO();
        testCustomerUpdate.setName("Updated Customer");
        testCustomerUpdate.setPhoneNumber("9876543210");
    }

    // ========== GET /api/v1/customers ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllCustomers_Success() throws Exception {
        // Arrange
        List<CustomerResponseDTO> customers = Arrays.asList(testCustomerResponse);
        when(customerService.getAll()).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customers retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].customerId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Test Customer"));

        verify(customerService, times(1)).getAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllCustomers_AsAdmin_Success() throws Exception {
        // Arrange
        List<CustomerResponseDTO> customers = Arrays.asList(testCustomerResponse);
        when(customerService.getAll()).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(customerService, times(1)).getAll();
    }

    @Test
    void testGetAllCustomers_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).getAll();
    }

    // ========== GET /api/v1/customers/me ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCurrentCustomer_Success() throws Exception {
        // Arrange
        when(customerService.getCustomer()).thenReturn(testCustomerResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.customerId").value(1))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(customerService, times(1)).getCustomer();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCurrentCustomer_NotFound() throws Exception {
        // Arrange
        when(customerService.getCustomer())
                .thenThrow(new CustomerNotFoundException("Customer not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/customers/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCustomerById_Success() throws Exception {
        // Arrange
        when(customerService.getById(1L)).thenReturn(testCustomerResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.customerId").value(1));

        verify(customerService, times(1)).getById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCustomerById_NotFound() throws Exception {
        // Arrange
        when(customerService.getById(999L))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCustomerById_AsAdmin_Success() throws Exception {
        // Arrange
        when(customerService.getById(1L)).thenReturn(testCustomerResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk());

        verify(customerService, times(1)).getById(1L);
    }

    // ========== POST /api/v1/customers ==========
    @Test
    @WithMockUser(roles = "PUBLIC")
    void testCreateCustomer_Success() throws Exception {
        // Arrange
        when(customerService.create(any(CustomerRequestDTO.class)))
                .thenReturn(testCustomerResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customer created successfully"))
                .andExpect(jsonPath("$.data.customerId").value(1));

        verify(customerService, times(1)).create(any(CustomerRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "PUBLIC")
    void testCreateCustomer_InvalidPhoneNumber() throws Exception {
        // Arrange
        CustomerRequestDTO invalidRequest = new CustomerRequestDTO();
        invalidRequest.setPhoneNumber("invalid"); // Invalid phone number

        // Act & Assert
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).create(any());
    }

    @Test
    void testCreateCustomer_NoAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomerRequest)))
                .andExpect(status().isUnauthorized());

        verify(customerService, never()).create(any());
    }

    // ========== PATCH /api/v1/customers/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testUpdateCustomer_Success() throws Exception {
        // Arrange
        CustomerResponseDTO updatedResponse = new CustomerResponseDTO();
        updatedResponse.setCustomerId(1L);
        updatedResponse.setName("Updated Customer");
        
        when(customerService.update(anyLong(), any(CustomerUpdateDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomerUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customer updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Customer"));

        verify(customerService, times(1)).update(anyLong(), any(CustomerUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testUpdateCustomer_NotFound() throws Exception {
        // Arrange
        when(customerService.update(anyLong(), any(CustomerUpdateDTO.class)))
                .thenThrow(new CustomerNotFoundException("Customer not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/customers/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCustomerUpdate)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /api/v1/customers/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeleteCustomer_Success() throws Exception {
        // Arrange
        doNothing().when(customerService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));

        verify(customerService, times(1)).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeleteCustomer_NotFound() throws Exception {
        // Arrange
        doThrow(new CustomerNotFoundException("Customer not found"))
                .when(customerService).delete(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/customers/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/customers/{id}/header ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetHeaderInfo_Success() throws Exception {
        // Arrange
        CustomerHeaderDTO headerDTO = CustomerHeaderDTO.builder()
                .name("Test Customer")
                .profileImage("profile.jpg")
                .build();
        
        when(customerService.getHeaderInfo(1L)).thenReturn(headerDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/1/header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("Test Customer"))
                .andExpect(jsonPath("$.data.profileImage").value("profile.jpg"));

        verify(customerService, times(1)).getHeaderInfo(1L);
    }

    // ========== GET /api/v1/customers/{id}/dashboard ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetDashboard_Success() throws Exception {
        // Arrange
        CustomerDashboardDTO dashboardDTO = CustomerDashboardDTO.builder()
                .profile(null)
                .summary(null)
                .recentActivities(Arrays.asList())
                .vehicles(Arrays.asList())
                .build();
        
        when(customerService.getDashboard(1L)).thenReturn(dashboardDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(customerService, times(1)).getDashboard(1L);
    }

    // ========== PUT /api/v1/customers/{id}/deactivate ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeactivateCustomer_Success() throws Exception {
        // Arrange
        doNothing().when(customerService).deactivateCustomer(anyLong(), anyString());

        String requestBody = "{\"reason\": \"Policy violation\"}";

        // Act & Assert
        mockMvc.perform(put("/api/v1/customers/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customer account deactivated successfully"));

        verify(customerService, times(1)).deactivateCustomer(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeactivateCustomer_Forbidden_AsCustomer() throws Exception {
        // Act & Assert - Customers should not be able to deactivate
        mockMvc.perform(put("/api/v1/customers/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        verify(customerService, never()).deactivateCustomer(anyLong(), anyString());
    }

    // ========== PUT /api/v1/customers/{id}/reactivate ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testReactivateCustomer_Success() throws Exception {
        // Arrange
        doNothing().when(customerService).reactivateCustomer(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/customers/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Customer account reactivated successfully"));

        verify(customerService, times(1)).reactivateCustomer(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testReactivateCustomer_Forbidden_AsCustomer() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/customers/1/reactivate"))
                .andExpect(status().isForbidden());

        verify(customerService, never()).reactivateCustomer(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testReactivateCustomer_NotFound() throws Exception {
        // Arrange
        doThrow(new CustomerNotFoundException("Customer not found"))
                .when(customerService).reactivateCustomer(999L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/customers/999/reactivate"))
                .andExpect(status().isNotFound());
    }
}
