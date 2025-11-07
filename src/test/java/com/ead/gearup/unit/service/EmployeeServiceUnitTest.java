package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.EmployeeDTOConverter;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceUnitTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EmployeeDTOConverter converter;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    private EmployeeService employeeService;
    private AutoCloseable closeable;

    private Employee testEmployee;
    private User testUser;
    private CreateEmployeeDTO createDTO;
    private UpdateEmployeeDTO updateDTO;
    private EmployeeResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        
        // Manually create the service with mocked dependencies
        employeeService = new EmployeeService(
            currentUserService,
            userRepository
        );
        
        // Use reflection to inject the @Autowired fields
        try {
            java.lang.reflect.Field repoField = EmployeeService.class.getDeclaredField("employeeRepository");
            repoField.setAccessible(true);
            repoField.set(employeeService, employeeRepository);
            
            java.lang.reflect.Field apptRepoField = EmployeeService.class.getDeclaredField("appointmentRepository");
            apptRepoField.setAccessible(true);
            apptRepoField.set(employeeService, appointmentRepository);
            
            java.lang.reflect.Field convField = EmployeeService.class.getDeclaredField("converter");
            convField.setAccessible(true);
            convField.set(employeeService, converter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("employee@example.com");
        testUser.setName("John Employee");
        testUser.setRole(UserRole.PUBLIC);

        // Setup test employee
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);
        testEmployee.setUser(testUser);
        testEmployee.setSpecialization("Mechanic");
        testEmployee.setHireDate(LocalDate.now());
        testEmployee.setPhoneNumber("1234567890");

        // Setup DTOs
        createDTO = new CreateEmployeeDTO();
        createDTO.setSpecialization("Mechanic");
        createDTO.setPhoneNumber("1234567890");

        updateDTO = new UpdateEmployeeDTO();
        updateDTO.setSpecialization("Senior Mechanic");

        responseDTO = new EmployeeResponseDTO();
        responseDTO.setEmployeeId(1L);
        responseDTO.setSpecialization("Mechanic");
    }

    // ========== createEmployee() Tests ==========
    @Test
    void testCreateEmployee_Success() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(converter.convertToEntity(any())).thenReturn(testEmployee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(converter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        EmployeeResponseDTO result = employeeService.createEmployee(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Mechanic", result.getSpecialization());
        assertEquals(UserRole.EMPLOYEE, testUser.getRole());
        verify(userRepository, times(1)).save(testUser);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_NoAuthenticatedUser() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> employeeService.createEmployee(createDTO));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testCreateEmployee_UserNotPublicRole() {
        // Arrange
        testUser.setRole(UserRole.CUSTOMER);
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(UnauthorizedCustomerAccessException.class, 
            () -> employeeService.createEmployee(createDTO));
        verify(employeeRepository, never()).save(any());
    }

    // ========== getAllEmployees() Tests ==========
    @Test
    void testGetAllEmployees_Success() {
        // Arrange
        List<Employee> employees = Arrays.asList(testEmployee);
        when(employeeRepository.findAll()).thenReturn(employees);
        when(converter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        List<EmployeeResponseDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    void testGetAllEmployees_EmptyList() {
        // Arrange
        when(employeeRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<EmployeeResponseDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== getEmployeeById() Tests ==========
    @Test
    void testGetEmployeeById_Success() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(converter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        EmployeeResponseDTO result = employeeService.getEmployeeById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getEmployeeId());
        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    void testGetEmployeeById_NotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(999L));
    }

    @Test
    void testGetEmployeeById_InvalidId_Null() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeeById(null));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void testGetEmployeeById_InvalidId_Zero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeeById(0L));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void testGetEmployeeById_InvalidId_Negative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeeById(-1L));
        verify(employeeRepository, never()).findById(any());
    }

    // ========== updateEmployee() Tests ==========
    @Test
    void testUpdateEmployee_Success() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        doNothing().when(converter).updateEntityFromDto(any(), any());
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        when(converter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        EmployeeResponseDTO result = employeeService.updateEmployee(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(employeeRepository, times(1)).save(testEmployee);
        verify(converter, times(1)).updateEntityFromDto(testEmployee, updateDTO);
    }

    @Test
    void testUpdateEmployee_NotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(999L, updateDTO));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testUpdateEmployee_InvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(null, updateDTO));
        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(0L, updateDTO));
        verify(employeeRepository, never()).save(any());
    }

    // ========== checkEmployeeDependencies() Tests ==========
    @Test
    void testCheckEmployeeDependencies_NoDependencies() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(appointmentRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        Map<String, Object> result = employeeService.checkEmployeeDependencies(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.get("appointmentCount"));
        assertEquals(true, result.get("canDelete"));
        assertEquals(false, result.get("hasAppointments"));
    }

    @Test
    void testCheckEmployeeDependencies_EmployeeNotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, 
            () -> employeeService.checkEmployeeDependencies(999L));
    }

    @Test
    void testCheckEmployeeDependencies_InvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.checkEmployeeDependencies(null));
        assertThrows(IllegalArgumentException.class, 
            () -> employeeService.checkEmployeeDependencies(-1L));
    }

    // ========== deleteEmployee() Tests ==========
    @Test
    void testDeleteEmployee_Success() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(appointmentRepository.findAll()).thenReturn(Arrays.asList());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(employeeRepository).delete(testEmployee);

        // Act
        employeeService.deleteEmployee(1L);

        // Assert
        assertEquals(UserRole.PUBLIC, testUser.getRole());
        verify(userRepository, times(1)).save(testUser);
        verify(employeeRepository, times(1)).delete(testEmployee);
    }

    @Test
    void testDeleteEmployee_NotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(999L));
        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void testDeleteEmployee_InvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> employeeService.deleteEmployee(null));
        assertThrows(IllegalArgumentException.class, () -> employeeService.deleteEmployee(0L));
        verify(employeeRepository, never()).delete(any());
    }

    // ========== getCurrentEmployee() Tests ==========
    @Test
    void testGetCurrentEmployee_Success() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(converter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        EmployeeResponseDTO result = employeeService.getCurrentEmployee();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getEmployeeId());
        verify(currentUserService, times(1)).getCurrentEntityId();
    }

    @Test
    void testGetCurrentEmployee_NotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getCurrentEmployee());
    }
}
