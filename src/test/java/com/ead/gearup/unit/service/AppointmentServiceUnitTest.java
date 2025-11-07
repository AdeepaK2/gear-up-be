package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AppointmentNotFoundException;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedAppointmentAccessException;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.AppointmentDTOConverter;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceUnitTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AppointmentDTOConverter converter;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment testAppointment;
    private Customer testCustomer;
    private Vehicle testVehicle;
    private AppointmentCreateDTO createDTO;
    private AppointmentUpdateDTO updateDTO;
    private AppointmentResponseDTO responseDTO;

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

        // Setup test appointment
        testAppointment = new Appointment();
        testAppointment.setAppointmentId(1L);
        testAppointment.setCustomer(testCustomer);
        testAppointment.setVehicle(testVehicle);
        testAppointment.setDate(LocalDate.now().plusDays(5));
        testAppointment.setStartTime(LocalTime.of(10, 0));
        testAppointment.setEndTime(LocalTime.of(12, 0));
        testAppointment.setStatus(AppointmentStatus.PENDING);

        // Setup DTOs
        createDTO = new AppointmentCreateDTO();
        createDTO.setVehicleId(1L);
        createDTO.setAppointmentDate(LocalDate.now().plusDays(5));
        createDTO.setStartTime(LocalTime.of(10, 0));
        createDTO.setEndTime(LocalTime.of(12, 0));

        updateDTO = new AppointmentUpdateDTO();
        updateDTO.setStartTime(LocalTime.of(11, 0));
        updateDTO.setEndTime(LocalTime.of(13, 0));

        responseDTO = new AppointmentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setAppointmentDate(LocalDate.now().plusDays(5));
    }

    // ========== createAppointment() Tests ==========
    @Test
    void testCreateAppointment_Success() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(converter.convertToEntity(any(), any(), any())).thenReturn(testAppointment);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        AppointmentResponseDTO result = appointmentService.createAppointment(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void testCreateAppointment_CustomerNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> appointmentService.createAppointment(createDTO));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testCreateAppointment_VehicleNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VehicleNotFoundException.class, () -> appointmentService.createAppointment(createDTO));
        verify(appointmentRepository, never()).save(any());
    }

    // ========== getAllAppointmentsForCurrentCustomer() Tests ==========
    @Test
    void testGetAllAppointmentsForCurrentCustomer_Success() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAllAppointmentsForCurrentCustomer();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findByCustomer(testCustomer);
    }

    @Test
    void testGetAllAppointmentsForCurrentCustomer_CustomerNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, 
            () -> appointmentService.getAllAppointmentsForCurrentCustomer());
    }

    // ========== getAppointmentById() Tests ==========
    @Test
    void testGetAppointmentById_Success() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(converter.convertToResponseDto(testAppointment)).thenReturn(responseDTO);

        // Act
        AppointmentResponseDTO result = appointmentService.getAppointmentById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(appointmentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAppointmentById_NotFound() {
        // Arrange
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class, () -> appointmentService.getAppointmentById(999L));
    }

    // ========== updateAppointment() Tests ==========
    @Test
    void testUpdateAppointment_Success() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(converter.updateEntityFromDto(any(), any())).thenReturn(testAppointment);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        AppointmentResponseDTO result = appointmentService.updateAppointment(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(appointmentRepository, times(1)).save(testAppointment);
    }

    @Test
    void testUpdateAppointment_InvalidTimeRange() {
        // Arrange
        updateDTO.setStartTime(LocalTime.of(14, 0));
        updateDTO.setEndTime(LocalTime.of(10, 0)); // End before start
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> appointmentService.updateAppointment(1L, updateDTO));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testUpdateAppointment_UnauthorizedCustomer() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(999L); // Different customer

        // Act & Assert
        assertThrows(UnauthorizedAppointmentAccessException.class, 
            () -> appointmentService.updateAppointment(1L, updateDTO));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testUpdateAppointment_NotFound() {
        // Arrange
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class, 
            () -> appointmentService.updateAppointment(999L, updateDTO));
    }

    // ========== deleteAppointment() Tests ==========
    @Test
    void testDeleteAppointment_Success() {
        // Arrange
        when(appointmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(appointmentRepository).deleteById(1L);

        // Act
        appointmentService.deleteAppointment(1L);

        // Assert
        verify(appointmentRepository, times(1)).existsById(1L);
        verify(appointmentRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteAppointment_NotFound() {
        // Arrange
        when(appointmentRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class, () -> appointmentService.deleteAppointment(999L));
        verify(appointmentRepository, never()).deleteById(any());
    }

    // ========== getAppointmentsByCustomerId() Tests ==========
    @Test
    void testGetAppointmentsByCustomerId_Success() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByCustomerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAppointmentsByCustomerId_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, 
            () -> appointmentService.getAppointmentsByCustomerId(999L));
    }

    // ========== getAvailableAppointmentsByCustomerId() Tests ==========
    @Test
    void testGetAvailableAppointmentsByCustomerId_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.PENDING);
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAvailableAppointmentsByCustomerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAvailableAppointmentsByCustomerId_FilterNonPending() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAvailableAppointmentsByCustomerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========== getUpcomingAppointmentsByCustomerId() Tests ==========
    @Test
    void testGetUpcomingAppointmentsByCustomerId_Success() {
        // Arrange
        testAppointment.setDate(LocalDate.now().plusDays(5));
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getUpcomingAppointmentsByCustomerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetUpcomingAppointmentsByCustomerId_FilterPastDates() {
        // Arrange
        testAppointment.setDate(LocalDate.now().minusDays(5)); // Past date
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(appointmentRepository.findByCustomer(testCustomer)).thenReturn(appointments);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getUpcomingAppointmentsByCustomerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========== getAllAppointments() Tests ==========
    @Test
    void testGetAllAppointments_Success() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentRepository.findAll()).thenReturn(appointments);
        when(converter.convertToResponseDto(any(Appointment.class))).thenReturn(responseDTO);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAllAppointments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findAll();
    }

    @Test
    void testGetAllAppointments_EmptyList() {
        // Arrange
        when(appointmentRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAllAppointments();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
