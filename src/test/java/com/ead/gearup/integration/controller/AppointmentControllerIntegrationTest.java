package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.employee.EmployeeAvailableSlotsDTO;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.exception.AppointmentNotFoundException;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.VehicleService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class AppointmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectMapper objectMapper;

    private AppointmentResponseDTO testAppointmentResponse;
    private AppointmentCreateDTO testAppointmentCreate;
    private AppointmentUpdateDTO testAppointmentUpdate;

    @BeforeEach
    void setUp() {
        testAppointmentResponse = new AppointmentResponseDTO();
        testAppointmentResponse.setId(1L);
        testAppointmentResponse.setCustomerIssue("Oil change");
        testAppointmentResponse.setStatus("PENDING");
        testAppointmentResponse.setAppointmentDate(LocalDate.now().plusDays(1));
        testAppointmentResponse.setStartTime(LocalDate.now().atTime(10, 0).toLocalTime());
        testAppointmentResponse.setEndTime(LocalDate.now().atTime(12, 0).toLocalTime());
        testAppointmentResponse.setConsultationType("GENERAL");

        testAppointmentCreate = new AppointmentCreateDTO();
        testAppointmentCreate.setCustomerIssue("Oil change");
        testAppointmentCreate.setVehicleId(1L);
        testAppointmentCreate.setAppointmentDate(LocalDate.now().plusDays(1));
        testAppointmentCreate.setStartTime(LocalDate.now().atTime(10, 0).toLocalTime());
        testAppointmentCreate.setEndTime(LocalDate.now().atTime(12, 0).toLocalTime());
        testAppointmentCreate.setConsultationType("GENERAL");

        testAppointmentUpdate = new AppointmentUpdateDTO();
        testAppointmentUpdate.setCustomerIssue("Updated issue");
        testAppointmentUpdate.setStatus(AppointmentStatus.IN_PROGRESS);
    }

    // ========== POST /api/v1/appointments ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateAppointment_Success() throws Exception {
        // Arrange
        when(appointmentService.createAppointment(any(AppointmentCreateDTO.class)))
                .thenReturn(testAppointmentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAppointmentCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Appointment created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.description").value("Oil change"));

        verify(appointmentService, times(1)).createAppointment(any(AppointmentCreateDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateAppointment_InvalidTimeRange() throws Exception {
        // Arrange
        when(appointmentService.createAppointment(any(AppointmentCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("End time must be after start time"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAppointmentCreate)))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/appointments ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllAppointments_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = Arrays.asList(testAppointmentResponse);
        when(appointmentService.getAllAppointmentsForCurrentCustomer()).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(appointmentService, times(1)).getAllAppointmentsForCurrentCustomer();
    }

    // ========== GET /api/v1/appointments/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAppointmentById_Success() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById(1L)).thenReturn(testAppointmentResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(appointmentService, times(1)).getAppointmentById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAppointmentById_NotFound() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById(999L))
                .thenThrow(new AppointmentNotFoundException("Appointment not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/999"))
                .andExpect(status().isNotFound());
    }

    // ========== PATCH /api/v1/appointments/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateAppointment_Success() throws Exception {
        // Arrange
        AppointmentResponseDTO updatedResponse = new AppointmentResponseDTO();
        updatedResponse.setId(1L);
        updatedResponse.setCustomerIssue("Updated issue");
        updatedResponse.setStatus("IN_PROGRESS");

        when(appointmentService.updateAppointment(anyLong(), any(AppointmentUpdateDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/appointments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAppointmentUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        verify(appointmentService, times(1)).updateAppointment(anyLong(), any(AppointmentUpdateDTO.class));
    }

    // ========== DELETE /api/v1/appointments/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testDeleteAppointment_Success() throws Exception {
        // Arrange
        doNothing().when(appointmentService).deleteAppointment(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Appointment deleted successfully"));

        verify(appointmentService, times(1)).deleteAppointment(1L);
    }

    // ========== GET /api/v1/appointments/customer/{customerId} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAppointmentsByCustomerId_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = Arrays.asList(testAppointmentResponse);
        when(appointmentService.getAppointmentsByCustomerId(1L)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray());

        verify(appointmentService, times(1)).getAppointmentsByCustomerId(1L);
    }

    // ========== GET /api/v1/appointments/customer/{customerId}/available ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAvailableAppointmentsByCustomerId_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = Arrays.asList(testAppointmentResponse);
        when(appointmentService.getAvailableAppointmentsByCustomerId(1L)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/customer/1/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).getAvailableAppointmentsByCustomerId(1L);
    }

    // ========== GET /api/v1/appointments/customer/{customerId}/upcoming ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUpcomingAppointmentsByCustomerId_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = Arrays.asList(testAppointmentResponse);
        when(appointmentService.getUpcomingAppointmentsByCustomerId(1L)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/customer/1/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).getUpcomingAppointmentsByCustomerId(1L);
    }

    // ========== GET /api/v1/appointments/employee ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAppointmentsForEmployee_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = Arrays.asList(testAppointmentResponse);
        when(appointmentService.getAppointmentsForEmployee()).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee appointments retrieved successfully"));

        verify(appointmentService, times(1)).getAppointmentsForEmployee();
    }

    // ========== GET /api/v1/appointments/vehicles ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetVehiclesForAppointments_Success() throws Exception {
        // Arrange
        VehicleResponseDTO vehicle = new VehicleResponseDTO();
        vehicle.setId(1L);
        vehicle.setMake("Toyota");
        List<VehicleResponseDTO> vehicles = Arrays.asList(vehicle);
        when(vehicleService.getVehiclesForCurrentCustomer()).thenReturn(vehicles);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].make").value("Toyota"));

        verify(vehicleService, times(1)).getVehiclesForCurrentCustomer();
    }

    // ========== GET /api/v1/appointments/filter-by-date ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAppointmentsByDate_Success() throws Exception {
        // Arrange
        LocalDate date = LocalDate.now();
        Long employeeId = 1L;
        when(currentUserService.getCurrentEntityId()).thenReturn(employeeId);
        when(appointmentService.getAppointmentsByDate(employeeId, date))
                .thenReturn(Arrays.asList(testAppointmentResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/filter-by-date")
                .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).getAppointmentsByDate(employeeId, date);
    }

    // ========== GET /api/v1/appointments/by-month ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAppointmentsByMonthAndStatuses_Success() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentsByMonthANDStatuses(anyInt(), anyInt(), anyList()))
                .thenReturn(Arrays.asList(testAppointmentResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/by-month")
                .param("year", "2024")
                .param("month", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1))
                .getAppointmentsByMonthANDStatuses(anyInt(), anyInt(), anyList());
    }

    // ========== GET /api/v1/appointments/search ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testSearchAppointments_Success() throws Exception {
        // Arrange
        when(appointmentService.searchAppointments("oil")).thenReturn(Arrays.asList(testAppointmentResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/search")
                .param("keyword", "oil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).searchAppointments("oil");
    }

    // ========== GET /api/v1/appointments/employee/upcoming ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetUpcomingAppointmentsForEmployee_Success() throws Exception {
        // Arrange
        when(appointmentService.getUpcomingAppointmentsForEmployee())
                .thenReturn(Arrays.asList(testAppointmentResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/employee/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).getUpcomingAppointmentsForEmployee();
    }

    // ========== GET /api/v1/appointments/employee/available-slots ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetEmployeeAvailableSlots_Success() throws Exception {
        // Arrange
        LocalDate date = LocalDate.now();
        EmployeeAvailableSlotsDTO slot = new EmployeeAvailableSlotsDTO();
        when(appointmentService.getAvailableSlotsForEmployee(date))
                .thenReturn(Arrays.asList(slot));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/employee/available-slots")
                .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(appointmentService, times(1)).getAvailableSlotsForEmployee(date);
    }
}
