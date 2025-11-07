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

        try {
            if(project.getAppointment() != null){
                dto.setAppointmentId(project.getAppointment().getAppointmentId());
            }
        } catch (Exception e) {
            System.err.println("Error accessing appointment: " + e.getMessage());
        }

        try {
            if(project.getVehicle() != null){
                dto.setVehicleId(project.getVehicle().getVehicleId());
                String make = project.getVehicle().getMake();
                String model = project.getVehicle().getModel();
                String vehicleName = null;
                if (make != null && !make.isEmpty() && model != null && !model.isEmpty()) {
                    vehicleName = make + " " + model;
                } else if (model != null && !model.isEmpty()) {
                    vehicleName = model;
                } else if (make != null && !make.isEmpty()) {
                    vehicleName = make;
                }
                dto.setVehicleName(vehicleName);
            }
        } catch (Exception e) {
            System.err.println("Error accessing vehicle: " + e.getMessage());
        }

        try {
            if(project.getCustomer() != null){
                dto.setCustomerId(project.getCustomer().getCustomerId());

                try {
                    if (project.getCustomer().getUser() != null) {
                        dto.setCustomerName(project.getCustomer().getUser().getName());
                    }
                } catch (Exception e) {
                    System.err.println("Error accessing customer user: " + e.getMessage());
                    dto.setCustomerName("Unknown");
                }
            }
        } catch (Exception e) {
            System.err.println("Error accessing customer: " + e.getMessage());
        }

        try {
            if (project.getTasks() != null) {
                List<Long> taskIds = project.getTasks().stream()
                        .map(Task::getTaskId)
                        .toList();
                System.out.println("Converting project " + project.getProjectId() + " with " + taskIds.size() + " tasks: " + taskIds);
                dto.setTaskIds(taskIds);
            } else {
                System.out.println("Project " + project.getProjectId() + " has null tasks collection");
            }
        } catch (Exception e) {
            System.err.println("Error accessing tasks for project " + project.getProjectId() + ": " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (project.getAssignedEmployees() != null) {
                List<Long> assignedEmployeeIds = project.getAssignedEmployees().stream()
                        .map(employee -> employee.getEmployeeId())
                        .toList();
                dto.setAssignedEmployeeIds(assignedEmployeeIds);
            } else {
                dto.setAssignedEmployeeIds(List.of());
            }
        } catch (Exception e) {
            System.err.println("Error accessing assigned employees for project " + project.getProjectId() + ": " + e.getMessage());
            dto.setAssignedEmployeeIds(List.of());
        }

        try {
            if (project.getMainRepresentativeEmployee() != null) {
                dto.setMainRepresentativeEmployeeId(project.getMainRepresentativeEmployee().getEmployeeId());
            } else {
                dto.setMainRepresentativeEmployeeId(null);
            }
        } catch (Exception e) {
            System.err.println("Error accessing main representative employee for project " + project.getProjectId() + ": " + e.getMessage());
            dto.setMainRepresentativeEmployeeId(null);
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
