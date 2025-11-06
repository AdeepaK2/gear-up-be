package com.ead.gearup.integration.graphql;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ead.gearup.controller.SearchGraphQLController;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseDTO;
import com.ead.gearup.dto.customer.CustomerSearchResponseDTO;
import com.ead.gearup.dto.employee.EmployeeSearchResponseDTO;
import com.ead.gearup.dto.task.TaskSearchResponseDTO;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.service.TaskService;

@SpringBootTest
@SuppressWarnings("removal")
class SearchGraphQLControllerTest {

    @Autowired
    private SearchGraphQLController searchGraphQLController;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private TaskService taskService;

    private AppointmentSearchResponseDTO testAppointment;
    private CustomerSearchResponseDTO testCustomer;
    private EmployeeSearchResponseDTO testEmployee;
    private TaskSearchResponseDTO testTask;

    @BeforeEach
    void setUp() {
        testAppointment = new AppointmentSearchResponseDTO();
        testAppointment.setAppointmentId(1L);
        testAppointment.setDate(LocalDate.now());
        testAppointment.setStatus("PENDING");
        testAppointment.setStartTime(LocalTime.of(10, 0));
        testAppointment.setEndTime(LocalTime.of(12, 0));

        testCustomer = new CustomerSearchResponseDTO();
        testCustomer.setCustomerId(1L);
        testCustomer.setPhoneNumber("1234567890");

        testEmployee = new EmployeeSearchResponseDTO();
        testEmployee.setEmployeeId(1L);
        testEmployee.setSpecialization("Mechanic");
        testEmployee.setHireDate(LocalDate.now());

        testTask = new TaskSearchResponseDTO();
        testTask.setTaskId(1L);
        testTask.setName("Oil Change");
        testTask.setDescription("Change engine oil");
        testTask.setStatus("PENDING");
    }

    // ========== searchAppointmentsByCustomerName ==========
    @Test
    void testSearchAppointmentsByCustomerName_Success() {
        // Arrange
        List<AppointmentSearchResponseDTO> appointments = Arrays.asList(testAppointment);
        when(appointmentService.searchAppointmentsByCustomerName("John")).thenReturn(appointments);

        // Act
        List<AppointmentSearchResponseDTO> result = 
            searchGraphQLController.searchAppointmentsByCustomerName("John");

        // Assert
        assert result != null;
        assert result.size() == 1;
        assert result.get(0).getAppointmentId().equals(1L);
        verify(appointmentService, times(1)).searchAppointmentsByCustomerName("John");
    }

    @Test
    void testSearchAppointmentsByCustomerName_EmptyResult() {
        // Arrange
        when(appointmentService.searchAppointmentsByCustomerName("NonExistent"))
                .thenReturn(Arrays.asList());

        // Act
        List<AppointmentSearchResponseDTO> result = 
            searchGraphQLController.searchAppointmentsByCustomerName("NonExistent");

        // Assert
        assert result != null;
        assert result.isEmpty();
        verify(appointmentService, times(1)).searchAppointmentsByCustomerName("NonExistent");
    }

    @Test
    void testSearchAppointmentsByCustomerName_MultipleResults() {
        // Arrange
        AppointmentSearchResponseDTO appointment2 = new AppointmentSearchResponseDTO();
        appointment2.setAppointmentId(2L);
        appointment2.setStatus("CONFIRMED");

        List<AppointmentSearchResponseDTO> appointments = Arrays.asList(testAppointment, appointment2);
        when(appointmentService.searchAppointmentsByCustomerName("John")).thenReturn(appointments);

        // Act
        List<AppointmentSearchResponseDTO> result = 
            searchGraphQLController.searchAppointmentsByCustomerName("John");

        // Assert
        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getAppointmentId().equals(1L);
        assert result.get(1).getAppointmentId().equals(2L);
        verify(appointmentService, times(1)).searchAppointmentsByCustomerName("John");
    }

    // ========== searchCustomersByCustomerName ==========
    @Test
    void testSearchCustomersByCustomerName_Success() {
        // Arrange
        List<CustomerSearchResponseDTO> customers = Arrays.asList(testCustomer);
        when(customerService.searchCustomersByCustomerName("John")).thenReturn(customers);

        // Act
        List<CustomerSearchResponseDTO> result = 
            searchGraphQLController.searchCustomersByCustomerName("John");

        // Assert
        assert result != null;
        assert result.size() == 1;
        assert result.get(0).getCustomerId().equals(1L);
        assert result.get(0).getPhoneNumber().equals("1234567890");
        verify(customerService, times(1)).searchCustomersByCustomerName("John");
    }

    @Test
    void testSearchCustomersByCustomerName_EmptyResult() {
        // Arrange
        when(customerService.searchCustomersByCustomerName(anyString()))
                .thenReturn(Arrays.asList());

        // Act
        List<CustomerSearchResponseDTO> result = 
            searchGraphQLController.searchCustomersByCustomerName("NonExistent");

        // Assert
        assert result != null;
        assert result.isEmpty();
    }

    @Test
    void testSearchCustomersByCustomerName_NullCheck() {
        // Arrange
        List<CustomerSearchResponseDTO> customers = Arrays.asList(testCustomer);
        when(customerService.searchCustomersByCustomerName("Test")).thenReturn(customers);

        // Act
        List<CustomerSearchResponseDTO> result = 
            searchGraphQLController.searchCustomersByCustomerName("Test");

        // Assert
        assert result != null;
        assert !result.isEmpty();
        assert result.get(0) != null;
    }

    // ========== searchEmployeesByEmployeeName ==========
    @Test
    void testSearchEmployeesByEmployeeName_Success() {
        // Arrange
        List<EmployeeSearchResponseDTO> employees = Arrays.asList(testEmployee);
        when(employeeService.searchEmployeesByEmployeeName("Jane")).thenReturn(employees);

        // Act
        List<EmployeeSearchResponseDTO> result = 
            searchGraphQLController.searchEmployeesByEmployeeName("Jane");

        // Assert
        assert result != null;
        assert result.size() == 1;
        assert result.get(0).getEmployeeId().equals(1L);
        assert result.get(0).getSpecialization().equals("Mechanic");
        verify(employeeService, times(1)).searchEmployeesByEmployeeName("Jane");
    }

    @Test
    void testSearchEmployeesByEmployeeName_EmptyResult() {
        // Arrange
        when(employeeService.searchEmployeesByEmployeeName("Unknown"))
                .thenReturn(Arrays.asList());

        // Act
        List<EmployeeSearchResponseDTO> result = 
            searchGraphQLController.searchEmployeesByEmployeeName("Unknown");

        // Assert
        assert result != null;
        assert result.isEmpty();
    }

    @Test
    void testSearchEmployeesByEmployeeName_MultipleResults() {
        // Arrange
        EmployeeSearchResponseDTO employee2 = new EmployeeSearchResponseDTO();
        employee2.setEmployeeId(2L);
        employee2.setSpecialization("Technician");

        List<EmployeeSearchResponseDTO> employees = Arrays.asList(testEmployee, employee2);
        when(employeeService.searchEmployeesByEmployeeName("Jane")).thenReturn(employees);

        // Act
        List<EmployeeSearchResponseDTO> result = 
            searchGraphQLController.searchEmployeesByEmployeeName("Jane");

        // Assert
        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getEmployeeId().equals(1L);
        assert result.get(1).getEmployeeId().equals(2L);
    }

    // ========== searchTasksByTaskName ==========
    @Test
    void testSearchTasksByTaskName_Success() {
        // Arrange
        List<TaskSearchResponseDTO> tasks = Arrays.asList(testTask);
        when(taskService.searchTasksByTaskName("Oil")).thenReturn(tasks);

        // Act
        List<TaskSearchResponseDTO> result = 
            searchGraphQLController.searchTasksByTaskName("Oil");

        // Assert
        assert result != null;
        assert result.size() == 1;
        assert result.get(0).getTaskId().equals(1L);
        assert result.get(0).getName().equals("Oil Change");
        verify(taskService, times(1)).searchTasksByTaskName("Oil");
    }

    @Test
    void testSearchTasksByTaskName_EmptyResult() {
        // Arrange
        when(taskService.searchTasksByTaskName("NonExistent"))
                .thenReturn(Arrays.asList());

        // Act
        List<TaskSearchResponseDTO> result = 
            searchGraphQLController.searchTasksByTaskName("NonExistent");

        // Assert
        assert result != null;
        assert result.isEmpty();
    }

    @Test
    void testSearchTasksByTaskName_VerifyServiceCall() {
        // Arrange
        List<TaskSearchResponseDTO> tasks = Arrays.asList(testTask);
        when(taskService.searchTasksByTaskName("Brake")).thenReturn(tasks);

        // Act
        searchGraphQLController.searchTasksByTaskName("Brake");

        // Assert
        verify(taskService, times(1)).searchTasksByTaskName("Brake");
        verify(taskService, never()).searchTasksByTaskName("Oil");
    }

    @Test
    void testSearchTasksByTaskName_MultipleResults() {
        // Arrange
        TaskSearchResponseDTO task2 = new TaskSearchResponseDTO();
        task2.setTaskId(2L);
        task2.setName("Brake Replacement");
        task2.setStatus("IN_PROGRESS");

        List<TaskSearchResponseDTO> tasks = Arrays.asList(testTask, task2);
        when(taskService.searchTasksByTaskName("Change")).thenReturn(tasks);

        // Act
        List<TaskSearchResponseDTO> result = 
            searchGraphQLController.searchTasksByTaskName("Change");

        // Assert
        assert result != null;
        assert result.size() == 2;
    }
}

