package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmployeeResponseDTO testEmployeeResponse;
    private CreateEmployeeDTO testEmployeeCreate;
    private UpdateEmployeeDTO testEmployeeUpdate;

    @BeforeEach
    void setUp() {
        testEmployeeResponse = new EmployeeResponseDTO();
        testEmployeeResponse.setEmployeeId(1L);
        testEmployeeResponse.setName("John Doe");
        testEmployeeResponse.setEmail("john.doe@example.com");

        testEmployeeCreate = new CreateEmployeeDTO();
        testEmployeeCreate.setSpecialization("Mechanic");
        testEmployeeCreate.setPhoneNumber("1234567890");

        testEmployeeUpdate = new UpdateEmployeeDTO();
        testEmployeeUpdate.setSpecialization("Senior Mechanic");
    }

    // ========== POST /api/v1/employees ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateEmployee_Success() throws Exception {
        // Arrange
        when(employeeService.createEmployee(any(CreateEmployeeDTO.class)))
                .thenReturn(testEmployeeResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployeeCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee created successfully"))
                .andExpect(jsonPath("$.data.employeeId").value(1))
                .andExpect(jsonPath("$.data.name").value("John Doe"));

        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateEmployee_DuplicateEmail() throws Exception {
        // Arrange
        when(employeeService.createEmployee(any(CreateEmployeeDTO.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployeeCreate)))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/employees ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllEmployees_Success() throws Exception {
        // Arrange
        List<EmployeeResponseDTO> employees = Arrays.asList(testEmployeeResponse);
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employees retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].employeeId").value(1));

        verify(employeeService, times(1)).getAllEmployees();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllEmployees_EmptyList() throws Exception {
        // Arrange
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== GET /api/v1/employees/{id} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetEmployeeById_Success() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById(1L)).thenReturn(testEmployeeResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee retrieved successfully"))
                .andExpect(jsonPath("$.data.employeeId").value(1));

        verify(employeeService, times(1)).getEmployeeById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetEmployeeById_NotFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById(999L))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/999"))
                .andExpect(status().isNotFound());
    }

    // ========== PATCH /api/v1/employees/{id} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateEmployee_Success() throws Exception {
        // Arrange
        EmployeeResponseDTO updatedResponse = new EmployeeResponseDTO();
        updatedResponse.setEmployeeId(1L);
        updatedResponse.setSpecialization("Senior Mechanic");

        when(employeeService.updateEmployee(anyLong(), any(UpdateEmployeeDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployeeUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee updated successfully"))
                .andExpect(jsonPath("$.data.specialization").value("Senior Mechanic"));

        verify(employeeService, times(1)).updateEmployee(anyLong(), any(UpdateEmployeeDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateEmployee_NotFound() throws Exception {
        // Arrange
        when(employeeService.updateEmployee(anyLong(), any(UpdateEmployeeDTO.class)))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/employees/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployeeUpdate)))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/employees/{id}/dependencies ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCheckEmployeeDependencies_Success() throws Exception {
        // Arrange
        Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("projects", 5L);
        dependencies.put("tasks", 10L);

        when(employeeService.checkEmployeeDependencies(1L)).thenReturn(dependencies);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/1/dependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee dependencies retrieved successfully"));

        verify(employeeService, times(1)).checkEmployeeDependencies(1L);
    }

    // ========== DELETE /api/v1/employees/{id} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteEmployee_Success() throws Exception {
        // Arrange
        doNothing().when(employeeService).deleteEmployee(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Employee deleted successfully"));

        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteEmployee_NotFound() throws Exception {
        // Arrange
        doThrow(new EmployeeNotFoundException("Employee not found"))
                .when(employeeService).deleteEmployee(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/employees/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteEmployee_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isUnauthorized());

        verify(employeeService, never()).deleteEmployee(anyLong());
    }

    // ========== GET /api/v1/employees/task-summary ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTaskSummaryForEmployee_Success() throws Exception {
        // Arrange
        Map<String, Long> taskSummary = new HashMap<>();
        taskSummary.put("PENDING", 5L);
        taskSummary.put("IN_PROGRESS", 3L);
        taskSummary.put("COMPLETED", 10L);

        when(taskService.getTaskSummaryForEmployee()).thenReturn(taskSummary);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/task-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task summary retrieved successfully"));

        verify(taskService, times(1)).getTaskSummaryForEmployee();
    }

    // ========== GET /api/v1/employees/me ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetCurrentEmployee_Success() throws Exception {
        // Arrange
        when(employeeService.getCurrentEmployee()).thenReturn(testEmployeeResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Current employee retrieved successfully"))
                .andExpect(jsonPath("$.data.employeeId").value(1));

        verify(employeeService, times(1)).getCurrentEmployee();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetCurrentEmployee_NotFound() throws Exception {
        // Arrange
        when(employeeService.getCurrentEmployee())
                .thenThrow(new EmployeeNotFoundException("Current employee not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isNotFound());
    }
}
