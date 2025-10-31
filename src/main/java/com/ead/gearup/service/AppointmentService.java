package com.ead.gearup.service;

import java.util.List;
import java.util.stream.Collectors;

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
}
