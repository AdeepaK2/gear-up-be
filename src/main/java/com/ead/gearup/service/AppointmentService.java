package com.ead.gearup.service;

import java.time.LocalDate;
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
import com.ead.gearup.entity.ShopSettings;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AppointmentNotFoundException;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedAppointmentAccessException;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.AppointmentDTOConverter;
import com.ead.gearup.validation.RequiresRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentService {

    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final AppointmentDTOConverter converter;
    private final AppointmentRepository appointmentRepository;
    private final ShopSettingsService shopSettingsService;

    @RequiresRole(UserRole.CUSTOMER)
    public AppointmentResponseDTO createAppointment(AppointmentCreateDTO appointmentCreateDTO) {
        // Validate appointment date is not in the past
        if (appointmentCreateDTO.getAppointmentDate() != null && appointmentCreateDTO.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot create appointment for a past date. Please select a current or future date.");
        }

        // Validate shop is open on the selected date
        if (appointmentCreateDTO.getAppointmentDate() != null && 
            !shopSettingsService.isShopOpenOnDate(appointmentCreateDTO.getAppointmentDate())) {
            throw new IllegalArgumentException("Shop is closed on the selected date. Please choose another date.");
        }

        // Validate appointment time is within operating hours
        if (appointmentCreateDTO.getStartTime() != null) {
            if (!shopSettingsService.isWithinOperatingHours(appointmentCreateDTO.getStartTime())) {
                ShopSettings settings = shopSettingsService.getShopSettingsEntity();
                throw new IllegalArgumentException(
                    String.format("Appointment time must be within shop operating hours (%s - %s).",
                        settings.getOpeningTime(), settings.getClosingTime())
                );
            }
        }

        if (appointmentCreateDTO.getEndTime() != null) {
            if (!shopSettingsService.isWithinOperatingHours(appointmentCreateDTO.getEndTime())) {
                ShopSettings settings = shopSettingsService.getShopSettingsEntity();
                throw new IllegalArgumentException(
                    String.format("Appointment end time must be within shop operating hours (%s - %s).",
                        settings.getOpeningTime(), settings.getClosingTime())
                );
            }
        }

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
        log.debug("Getting appointments for current customer");
        Long currentEntityId = currentUserService.getCurrentEntityId();
        log.debug("Current entity ID: {}", currentEntityId);
        
        Customer customer;
        
        if (currentEntityId == null) {
            log.warn("Current entity ID is null, trying to find customer by user");
            // Try to find customer by user instead
            User currentUser = currentUserService.getCurrentUser();
            log.debug("Current user: {} ({})", currentUser.getName(), currentUser.getEmail());
            
            customer = customerRepository.findByUser(currentUser)
                    .orElseThrow(() -> {
                        log.error("Customer profile not found for user: {} ({})", currentUser.getName(), currentUser.getEmail());
                        return new CustomerNotFoundException(
                                "Customer profile not found for user: " + currentUser.getEmail());
                    });
            
            log.debug("Found customer by user lookup: {}", customer.getCustomerId());
        } else {
            customer = customerRepository.findById(currentEntityId)
                    .orElseThrow(() -> {
                        log.error("Customer not found with ID: {}", currentEntityId);
                        return new CustomerNotFoundException(
                                "Customer not found with ID: " + currentEntityId);
                    });
            
            log.debug("Found customer by ID: {}", customer.getCustomerId());
        }

        List<Appointment> appointments = appointmentRepository.findByCustomer(customer);
        log.debug("Found {} appointments for customer {}", appointments.size(), customer.getCustomerId());

        return appointments.stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        log.debug("Getting appointment by ID: {}", appointmentId);
        
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + appointmentId));

        // Authorization check: only allow access to own appointments for customers
        UserRole currentRole = currentUserService.getCurrentUserRole();
        log.debug("Current user role: {}", currentRole);
        
        if (currentRole == UserRole.CUSTOMER) {
            Long currentCustomerId = currentUserService.getCurrentEntityId();
            log.debug("Current customer ID: {}, Appointment customer ID: {}", currentCustomerId, appointment.getCustomer().getCustomerId());
            
            if (currentCustomerId == null) {
                log.warn("Customer ID is null for authenticated customer");
                // Try fallback user lookup
                try {
                    User currentUser = currentUserService.getCurrentUser();
                    if (currentUser != null) {
                        Customer customer = customerRepository.findByUser(currentUser).orElse(null);
                        if (customer != null && !appointment.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                            throw new UnauthorizedAppointmentAccessException("You can only view your own appointments");
                        }
                    } else {
                        throw new UnauthorizedAppointmentAccessException("Unable to verify appointment ownership");
                    }
                } catch (Exception e) {
                    log.error("Error during fallback customer lookup: {}", e.getMessage());
                    throw new UnauthorizedAppointmentAccessException("Unable to verify appointment ownership");
                }
            } else if (!appointment.getCustomer().getCustomerId().equals(currentCustomerId)) {
                throw new UnauthorizedAppointmentAccessException("You can only view your own appointments");
            }
        }
        // Employees and admins can view any appointment
        
        log.debug("Authorization successful for appointment: {}", appointmentId);
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
