package com.ead.gearup.service;

import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.UnauthorizedAppointmentAccessException;
import com.ead.gearup.validation.RequiresRole;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.employee.EmployeeAvailableSlotsDTO;
import com.ead.gearup.exception.AppointmentNotFoundException;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.VehicleNotFoundException;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.VehicleRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.AppointmentDTOConverter;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @RequiresRole({UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE})
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

    @RequiresRole({UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE})
    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + appointmentId));

        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!appointment.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedAppointmentAccessException(
                        "You cannot access another customer's appointment: " + customerId);
            }
        }
        return converter.convertToResponseDto(appointment);

    }

    @RequiresRole({UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> getAllAppointments() {
        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            return appointmentRepository.findAll().stream()
                    .filter(a -> a.getCustomer().getCustomerId().equals(customerId))
                    .map(converter::convertToResponseDto)
                    .toList();
        }
        return appointmentRepository.findAll().stream()
                .map(converter::convertToResponseDto)
                .toList();
    }

    @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN })
    public void deleteAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + appointmentId));

        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!appointment.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedAppointmentAccessException(
                        "You cannot delete another customer's appointment: " + customerId);
            }
        }
        appointment.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.save(appointment);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<AppointmentResponseDTO> getUpcomingAppointmentsForEmployee(Long employeeId) {
        return appointmentRepository
                .findByEmployeeEmployeeIdAndDateGreaterThanEqualOrderByDateAsc(employeeId, LocalDate.now())
                .stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> getAppointmentsForEmployee(){
        Long employeeId = currentUserService.getCurrentEntityId();
        return appointmentRepository
                .findByEmployeeEmployeeId(employeeId)
                .stream()
                .map(converter::convertToResponseDto)
                .collect(Collectors.toList());
    }


    @RequiresRole({UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> getAppointmentsByDate(Long employeeId, LocalDate date) {
        return appointmentRepository
                .findByEmployeeEmployeeIdAndDate(employeeId, date)
                .stream()
                .map(converter::convertToResponseDto)
                .toList();
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> getAppointmentsByMonthANDStatuses(int year, int month, List<AppointmentStatus> statuses) {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByEmployeeAndMonthAndStatus(employeeId, year, month, statuses);
        return appointments.stream()
                .map(converter::convertToResponseDto)
                .toList();
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> searchAppointments(String keyword) {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Appointment> appointments = appointmentRepository
                .searchAppointmentsByCustomerNameOrTask(employeeId, keyword);

        return appointments.stream()
                .map(converter::convertToResponseDto)
                .toList();
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public List<AppointmentResponseDTO> getUpcomingAppointmentsForEmployee(){
        Long employeeId = currentUserService.getCurrentEntityId();
        return appointmentRepository
                .findByEmployeeEmployeeIdAndStatusAndDateAfter(employeeId, AppointmentStatus.CONFIRMED, LocalDate.now())
                .stream()
                 .map(converter::convertToResponseDto)
                 .collect(Collectors.toList());
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public List<EmployeeAvailableSlotsDTO> getAvailableSlotsForEmployee(LocalDate date){

        // Get upcoming appointments for current employee
        List<AppointmentResponseDTO> appointments = getUpcomingAppointmentsForEmployee();

        // Filter appointments for the given date
        List<AppointmentResponseDTO> appointmentsForDate = appointments.stream()
                .filter(a -> a.getDate().equals(date))
                .toList();

        // Generate all slots between 8 AM and 6 PM
        List<LocalTime> workingHours = new ArrayList<>();
        for(int hour = 8; hour < 19; hour++){
            workingHours.add(LocalTime.of(hour, 0));
        }

        // Remove slots that are booked in confiremed appointments
        List<LocalTime> bookedSlots = appointmentsForDate.stream()
                .filter(a -> "CONFIRMED".equalsIgnoreCase(a.getStatus()))
                .map(AppointmentResponseDTO::getStartTime)
                .toList();

        workingHours.removeAll(bookedSlots);

        return List.of(new EmployeeAvailableSlotsDTO(date, workingHours));
    }

    @RequiresRole(UserRole.CUSTOMER)
    public List<VehicleResponseDTO> getVehiclesForCurrentCustomer() {
        Long customerId = currentUserService.getCurrentEntityId();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        return customer.getVehicles().stream()
                .map(v -> new VehicleResponseDTO(
                        v.getVehicleId(),
                        v.getVin(),
                        v.getLicensePlate(),
                        v.getYear(),
                        v.getModel(),
                        v.getMake()
                ))
                .toList();
    }

    public List<AppointmentSearchResponseDTO> searchAppointmentsByCustomerName(String name) {
        return appointmentRepository.findAppointmentSearchResultsNative(name)
                .stream()
                .map(p -> new AppointmentSearchResponseDTO(
                        p.getAppointmentId(),
                        p.getDate(),
                        p.getStatus(),
                        p.getNotes(),
                        p.getStartTime(),
                        p.getEndTime()))
                .collect(Collectors.toList());
    }

}
