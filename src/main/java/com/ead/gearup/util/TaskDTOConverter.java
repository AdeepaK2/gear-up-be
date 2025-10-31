package com.ead.gearup.util;

import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskUpdateDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDTOConverter {

    private final AppointmentRepository appointmentRepository;

    // Convert TaskCreateDTO to Task entity
    public Task convertToEntity(TaskCreateDTO dto) {
        Task task = new Task();

        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        task.setEstimatedHours(dto.getEstimatedHours());
        task.setEstimatedCost(dto.getEstimatedCost());
        task.setAppointment(appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> {
                    throw new IllegalArgumentException("Invalid appointment ID: " + dto.getAppointmentId());
                }));

        return task;
    }

    // Convert Task entity to Response DTO
    public TaskResponseDTO convertToResponseDto(Task task) {
        TaskResponseDTO dto = new TaskResponseDTO();

        dto.setTaskId(task.getTaskId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setEstimatedHours(task.getEstimatedHours());
        dto.setEstimatedCost(task.getEstimatedCost());
        dto.setAppointmentId(task.getAppointment() != null ? task.getAppointment().getAppointmentId() : null);
        dto.setStatus(task.getStatus());
        dto.setCategory(task.getCategory());
        dto.setPriority(task.getPriority());
        dto.setNotes(task.getNotes());
        dto.setRequestedBy(task.getRequestedBy());
        dto.setCreatedAt(task.getCreatedAt());


        return dto;
    }

    // Update Task entity from TaskUpdateDTO (partial update)
    public Task updateEntityFromDto(Task task, TaskUpdateDTO dto) {
        if (dto.getName() != null) {
            task.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getEstimatedHours() != null) {
            task.setEstimatedHours(dto.getEstimatedHours());
        }
        if (dto.getEstimatedCost() != null) {
            task.setEstimatedCost(dto.getEstimatedCost());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        return task;
    }
}
