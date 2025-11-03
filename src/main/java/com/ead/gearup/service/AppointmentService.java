package com.ead.gearup.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseProjection;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.employee.EmployeeAvailableSlotsDTO;
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
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.AppointmentDTOConverter;
import com.ead.gearup.validation.RequiresRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final AppointmentDTOConverter converter;
    private final AppointmentRepository appointmentRepository;

    @RequiresRole(UserRole.CUSTOMER)
    public AppointmentResponseDTO createAppointment(AppointmentCreateDTO appointmentCreateDTO) {
        Customer customer = customerRepository.findById(currentUserService.getCurrentEntityId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found: " + currentUserService.getCurrentEntityId()));

        Vehicle vehicle = vehicleRepository.findById(appointmentCreateDTO.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found: " + appointmentCreateDTO.getVehicleId()));

        Appointment appointment = converter.convertToEntity(appointmentCreateDTO, vehicle, customer);
        appointmentRepository.save(appointment);

        return converter.convertToResponseDto(appointment);
    }

    public List<AppointmentResponseDTO> getAllAppointmentsForCurrentCustomer() {
        Customer customer = customerRepository.findById(currentUserService.getCurrentEntityId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found: " + currentUserService.getCurrentEntityId()));

        List<Appointment> appointments = appointmentRepository.findByCustomer(customer);

        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + appointmentId));

        return converter.convertToResponseDto(appointment);
    }

    public AppointmentResponseDTO updateAppointment(Long appointmentId, AppointmentUpdateDTO updateDTO) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + appointmentId));

        if (updateDTO.getStartTime() != null && updateDTO.getEndTime() != null) {
            if (updateDTO.getEndTime().isBefore(updateDTO.getStartTime())) {
                throw new IllegalArgumentException("End time cannot be before start time");
            }
        }

        if (currentUserService.getCurrentUserRole() == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!appointment.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedAppointmentAccessException("You cannot update another customer's appointment");
            }
        }

        Appointment updatedAppointment = converter.updateEntityFromDto(appointment, updateDTO);
        appointmentRepository.save(updatedAppointment);

        return converter.convertToResponseDto(updatedAppointment);
    }

    public void deleteAppointment(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new AppointmentNotFoundException("Appointment not found: " + appointmentId);
        }

        appointmentRepository.deleteById(appointmentId);
    }

    /**
     * Get appointments by specific customer ID (for chatbot/external services)
     */
    public List<AppointmentResponseDTO> getAppointmentsByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        List<Appointment> appointments = appointmentRepository.findByCustomer(customer);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get available appointments (PENDING status) for a customer
     */
    public List<AppointmentResponseDTO> getAvailableAppointmentsByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        List<Appointment> appointments = appointmentRepository.findByCustomer(customer)
                .stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING)
                .collect(Collectors.toList());

        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming appointments for a customer (future dates only)
     */
    public List<AppointmentResponseDTO> getUpcomingAppointmentsByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        List<Appointment> appointments = appointmentRepository.findByCustomer(customer)
                .stream()
                .filter(appointment -> appointment.getDate().isAfter(LocalDate.now()) || 
                       appointment.getDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // Additional methods needed by the controller
    public List<AppointmentResponseDTO> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> getUpcomingAppointmentsForEmployee(Long employeeId) {
        List<Appointment> appointments = appointmentRepository
                .findByEmployeeEmployeeIdAndDateGreaterThanEqualOrderByDateAsc(employeeId, LocalDate.now());
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> getAppointmentsForEmployee() {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository.findByEmployeeEmployeeId(employeeId);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> getAppointmentsByDate(Long employeeId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.findByEmployeeEmployeeIdAndDate(employeeId, date);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> getAppointmentsByMonthANDStatuses(int year, int month, 
            List<com.ead.gearup.enums.AppointmentStatus> statuses) {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByEmployeeAndMonthAndStatus(employeeId, year, month, statuses);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> searchAppointments(String keyword) {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository
                .searchAppointmentsByCustomerNameOrTask(employeeId, keyword);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponseDTO> getUpcomingAppointmentsForEmployee() {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository
                .findByEmployeeEmployeeIdAndDateGreaterThanEqualOrderByDateAsc(employeeId, LocalDate.now());
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public List<EmployeeAvailableSlotsDTO> getAvailableSlotsForEmployee(LocalDate date) {
        // This would need more complex logic to calculate available slots
        // For now, return empty list or implement basic slot calculation
        return new ArrayList<>();
    }

    public List<AppointmentSearchResponseDTO> searchAppointmentsByCustomerName(String customerName) {
        List<AppointmentSearchResponseProjection> projections = appointmentRepository
                .findAppointmentSearchResultsNative(customerName);
        return projections.stream()
                .map(this::convertProjectionToSearchResponse)
                .collect(Collectors.toList());
    }

    private AppointmentSearchResponseDTO convertProjectionToSearchResponse(AppointmentSearchResponseProjection projection) {
        AppointmentSearchResponseDTO dto = new AppointmentSearchResponseDTO();
        dto.setAppointmentId(projection.getAppointmentId());
        dto.setDate(projection.getDate());
        dto.setStatus(projection.getStatus());
        dto.setNotes(projection.getNotes());
        dto.setStartTime(projection.getStartTime());
        dto.setEndTime(projection.getEndTime());
        return dto;
    }
}
