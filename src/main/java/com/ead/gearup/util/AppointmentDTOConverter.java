package com.ead.gearup.util;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ead.gearup.dto.appointment.*;
import com.ead.gearup.enums.ConsultationType;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentDTOConverter {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

    /** Converts CreateDTO → Appointment entity */
    public Appointment convertToEntity(AppointmentCreateDTO dto, Vehicle vehicle, Customer customer) {
        Appointment appointment = new Appointment();

        appointment.setVehicle(vehicle);
        appointment.setCustomer(customer);
        appointment.setDate(dto.getAppointmentDate());
        appointment.setStartTime(dto.getStartTime());

        // Automatically calculate end time: start time + 1 hour
        if (dto.getEndTime() != null) {
            appointment.setEndTime(dto.getEndTime());
        } else if (dto.getStartTime() != null) {
            // Add 1 hour to start time
            appointment.setEndTime(dto.getStartTime().plusHours(1));
        }

        appointment.setNotes(dto.getNotes());
        appointment.setCustomerIssue(dto.getCustomerIssue());

        if (dto.getConsultationType() != null) {
            appointment.setConsultationType(ConsultationType.valueOf(dto.getConsultationType()));
        }

        return appointment;
    }

    /** Converts Appointment entity → ResponseDTO */
    public AppointmentResponseDTO convertToResponseDto(Appointment appointment) {
        Vehicle v = appointment.getVehicle();

        return AppointmentResponseDTO.builder()
                .id(appointment.getAppointmentId())
                .vehicleId(v.getVehicleId())
                .vehicleName(v.getMake() + " " + v.getModel())
                .vehicleDetails(v.getYear() + " | " + v.getLicensePlate())
                .customerId(appointment.getCustomer().getCustomerId())
                .employeeId(appointment.getEmployee() != null
                        ? appointment.getEmployee().getEmployeeId()
                        : null)
                .consultationType(appointment.getConsultationType() != null
                        ? appointment.getConsultationType().name()
                        : null)
                .consultationTypeLabel(appointment.getConsultationType() != null
                        ? appointment.getConsultationType().getLabel()
                        : null)
                .appointmentDate(appointment.getDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus().name())
                .customerIssue(appointment.getCustomerIssue())
                .notes(appointment.getNotes())
                .taskIds(appointment.getTasks() != null
                        ? appointment.getTasks().stream().map(Task::getTaskId).toList()
                        : List.of())
                .build();
    }

    /** Updates an existing Appointment from UpdateDTO */
    public Appointment updateEntityFromDto(Appointment appointment, AppointmentUpdateDTO dto) {

        if (dto.getAppointmentDate() != null)
            appointment.setDate(dto.getAppointmentDate());

        if (dto.getStartTime() != null)
            appointment.setStartTime(dto.getStartTime());

        if (dto.getEndTime() != null)
            appointment.setEndTime(dto.getEndTime());

        if (dto.getNotes() != null)
            appointment.setNotes(dto.getNotes());

        if (dto.getCustomerIssue() != null)
            appointment.setCustomerIssue(dto.getCustomerIssue());

        if (dto.getConsultationType() != null)
            appointment.setConsultationType(ConsultationType.valueOf(dto.getConsultationType()));

        if (dto.getStatus() != null)
            appointment.setStatus(dto.getStatus());

        if (dto.getEmployeeId() != null)
            appointment.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(
                            "Employee not found: " + dto.getEmployeeId())));

        if (dto.getTaskIds() != null) {
            if (!dto.getTaskIds().isEmpty()) {
                List<Task> newTasks = taskRepository.findAllById(dto.getTaskIds());

                if (newTasks.size() != dto.getTaskIds().size()) {
                    throw new IllegalArgumentException("One or more tasks not found: " + dto.getTaskIds());
                }

                // Clear existing tasks and add new ones to maintain the same collection instance
                appointment.getTasks().clear();
                newTasks.forEach(task -> {
                    task.setAppointment(appointment);
                    appointment.getTasks().add(task);
                });
            } else {
                // If empty list is provided, clear all tasks
                appointment.getTasks().clear();
            }
        }

        return appointment;
    }
}
