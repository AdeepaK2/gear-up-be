package com.ead.gearup.util;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.ProjectDetailsResponseDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.model.Project;
import com.ead.gearup.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectDTOConverter {
    public Project convertToEntity(CreateProjectDTO dto){
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        return project;
    }

    public void updateEntityFromDto(UpdateProjectDTO dto, Project project){
        if(dto.getName() != null){
            project.setName(dto.getName());
        }
        if(dto.getDescription() != null){
            project.setDescription(dto.getDescription());
        }
        if(dto.getEndDate() != null){
            project.setEndDate(dto.getEndDate());
        }
        if(dto.getStatus() != null){
            project.setStatus(dto.getStatus());
        }
    }

    public ProjectResponseDTO convertToResponseDto(Project project){
        ProjectResponseDTO dto = new ProjectResponseDTO();

        dto.setId(project.getProjectId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());

        if(project.getAppointment() != null){
            dto.setAppointmentId(project.getAppointment().getAppointmentId());
        }
        if(project.getVehicle() != null){
            dto.setVehicleId(project.getVehicle().getVehicleId());
        }
        if(project.getCustomer() != null){
            dto.setCustomerId(project.getCustomer().getCustomerId());
        }
        if (project.getTasks() != null) {
            dto.setTaskIds(project.getTasks().stream()
                    .map(Task::getTaskId)
                    .toList());
        }

        return dto;
    }

    public ProjectDetailsResponseDTO convertToDetailsResponseDto(Project project) {

        ProjectDetailsResponseDTO dto = new ProjectDetailsResponseDTO();

        dto.setId(project.getProjectId());
        dto.setStatus(project.getStatus());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        dto.setTotalAcceptedCost(project.getTotalAcceptedCost());
        dto.setAcceptedServicesCount(project.getAcceptedServicesCount());
        dto.setAdditionalRequest(project.getAdditionalRequest());
        dto.setReferenceFilePath(project.getReferenceFilePath());


        // Appointment details
        if (project.getAppointment() != null) {
            dto.setAppointmentId(project.getAppointment().getAppointmentId());
            dto.setConsultationType(project.getAppointment().getConsultationType() != null
                    ? project.getAppointment().getConsultationType().name() : null);
            dto.setConsultationDate(project.getAppointment().getDate());

            // Customer & employee
            if (project.getAppointment().getCustomer() != null) {
                dto.setCustomerId(project.getAppointment().getCustomer().getCustomerId());
            }
            if (project.getAppointment().getEmployee() != null) {
                dto.setEmployeeId(project.getAppointment().getEmployee().getEmployeeId());
                dto.setEmployeeName(project.getAppointment().getEmployee().getUser().getName());
            }

            // Vehicle
            if (project.getAppointment().getVehicle() != null) {
                var v = project.getAppointment().getVehicle();
                dto.setVehicleId(v.getVehicleId());
                dto.setVehicleName(v.getMake());
                dto.setVehicleDetails(v.getModel() + " " + v.getYear() + " - " + v.getLicensePlate());
            }
        }

        // Tasks / services
        if (project.getTasks() != null) {
            List<TaskResponseDTO> tasks = project.getTasks().stream()
                    .map(task -> new TaskResponseDTO(
                            task.getTaskId(),
                            task.getAppointment() != null ? task.getAppointment().getAppointmentId() : null, // ✅ 2nd param
                            task.getName(),
                            task.getDescription(),
                            task.getEstimatedHours(),
                            task.getEstimatedCost(),
                            task.getStatus(),
                            task.getCategory(),
                            task.getPriority(),
                            task.getNotes(),
                            task.getRequestedBy(),
                            task.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
            dto.setServices(tasks);

            dto.setTotalEstimatedCost(tasks.stream()
                    .mapToDouble(TaskResponseDTO::getEstimatedCost)
                    .sum());
            dto.setTotalAcceptedCost(tasks.stream()
                    .filter(t -> t.getStatus().name().equals("ACCEPTED"))
                    .mapToDouble(TaskResponseDTO::getEstimatedCost)
                    .sum());
            dto.setAcceptedServicesCount((int) tasks.stream()
                    .filter(t -> t.getStatus().name().equals("ACCEPTED"))
                    .count());
        }

        // Placeholder for additional requests (if not implemented yet)
        dto.setAdditionalRequests(List.of());

        return dto;
    }
}
