package com.ead.gearup.service;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.employee.EmployeeProjectDetailResponseDTO;
import com.ead.gearup.dto.employee.EmployeeProjectResponseDTO;
import com.ead.gearup.dto.project.*;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskStatusUpdateDTO;
import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.enums.TaskStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.*;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.*;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.AppointmentDTOConverter;
import com.ead.gearup.util.TaskDTOConverter;
import com.ead.gearup.validation.RequiresRole;

import com.ead.gearup.util.ProjectDTOConverter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final AppointmentRepository appointmentRepository;
    private final VehicleRepository vehicleRepository;
    private final TaskRepository taskRepository;
    private final ProjectDTOConverter projectDTOConverter;
    private final TaskDTOConverter taskDTOConverter;


    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO createProject(CreateProjectDTO dto) {
        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found: " + dto.getAppointmentId()));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found: " + dto.getVehicleId()));

        List<Task> tasks = taskRepository.findAllById(dto.getTaskIds());
        if (tasks.isEmpty()) {
            throw new TaskNotFoundException("No valid tasks found for project");
        }

        Project project = projectDTOConverter.convertToEntity(dto);
        project.setAppointment(appointment);
        project.setVehicle(vehicle);
        project.setTasks(tasks);
        project.setStatus(ProjectStatus.CREATED);

        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO updateProject(Long projectId, UpdateProjectDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectDTOConverter.updateEntityFromDto(dto, project);
        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!project.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedProjectAccessException("You cannot access this project.");
            }
        }

        if (role == UserRole.EMPLOYEE) {
            Long employeeId = currentUserService.getCurrentEntityId();
            boolean assigned = project.getAssignedEmployees().stream()
                    .anyMatch(e -> e.getEmployeeId().equals(employeeId));
            if (!assigned) {
                throw new UnauthorizedProjectAccessException("You are not assigned to this project.");
            }
        }

        return projectDTOConverter.convertToResponseDto(project);
    }

    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<ProjectResponseDTO> getAllProjects() {
        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            return projectRepository.findAll().stream()
                    .filter(p -> p.getCustomer().getCustomerId().equals(customerId))
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();
        }

        if (role == UserRole.EMPLOYEE) {
            Long employeeId = currentUserService.getCurrentEntityId();
            return projectRepository.findAll().stream()
                    .filter(p -> p.getAssignedEmployees().stream()
                            .anyMatch(e -> e.getEmployeeId().equals(employeeId)))
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();
        }

        // ADMIN → all projects
        return projectRepository.findAll().stream()
                .map(projectDTOConverter::convertToResponseDto)
                .toList();
    }

    @RequiresRole(UserRole.ADMIN)
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectRepository.delete(project);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public Map<String, Long> getProjectCountByStatus(Long employeeId) {
        List<Object[]> results = projectRepository.countProjectsByStatusForEmployee(employeeId);
        Map<String, Long> response = new HashMap<>();
    
        for (Object[] row : results) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            response.put(status, count);
        }
    
        return response;
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<EmployeeProjectResponseDTO> getAssignedProjectsForCurrentEmployee() {
        Long employeeId = currentUserService.getCurrentUserId();
        List<Project> projects = projectRepository.findByAssignedEmployeesEmployeeId(employeeId);

        return projects.stream()
                .map(p -> new EmployeeProjectResponseDTO(
                        p.getProjectId(),
                        p.getName()
                ))
                .toList();
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public EmployeeProjectDetailResponseDTO getAssignedProjectDetail(Long projectId) {
        Long employeeId = currentUserService.getCurrentUserId();

        Project project = projectRepository.findByProjectIdAndAssignedEmployeesEmployeeId(projectId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or not assigned to you: " + projectId));

        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();
        Long completionDays = null;

        if(startDate != null && endDate != null) {
            completionDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        }
                

        return new EmployeeProjectDetailResponseDTO(
                project.getCustomer().getUser().getName(),
                project.getVehicle().getModel(),
                project.getEndDate(),
                project.getStartDate(),
                project.getStatus(),
                completionDays
        );
    }
    @Transactional
    public TaskResponseDTO updateServiceStatus(Long projectId, Long taskId, TaskStatusUpdateDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        Task task = project.getTasks().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException("Task not found in this project: " + taskId));

        // Only allow specific statuses
        if (dto.getStatus() != TaskStatus.ACCEPTED && dto.getStatus() != TaskStatus.RECOMMENDED) {
            throw new IllegalArgumentException("Only ACCEPTED or RECOMMENDED statuses are allowed");
        }

        task.setStatus(dto.getStatus());
        taskRepository.save(task);

        return taskDTOConverter.convertToResponseDto(task);
    }

    @Transactional
    public ProjectResponseDTO confirmServices(Long projectId, ProjectConfirmDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        if (project.getStatus() == ProjectStatus.CONFIRMED) {
            throw new IllegalStateException("Project already confirmed");
        }

        // ✅ Update fields
        project.setStatus(ProjectStatus.CONFIRMED);
        project.setEndDate(LocalDate.now()); // optional
        project.setDescription("Project confirmed with " + dto.getAcceptedServicesCount() + " accepted services.");

        // ✅ Add totals (you can also calculate this from tasks)
        project.setTotalAcceptedCost(dto.getTotalAcceptedCost());
        project.setAcceptedServicesCount(dto.getAcceptedServicesCount());

        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @Transactional
    public ProjectResponseDTO addAdditionalServiceRequest(Long projectId, ProjectAdditionalRequestDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // (Optional) File saving logic
        String savedFilePath = null;
        if (dto.getReferenceFile() != null && !dto.getReferenceFile().isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads/project-requests");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String fileName = System.currentTimeMillis() + "_" + dto.getReferenceFile().getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                dto.getReferenceFile().transferTo(filePath.toFile());
                savedFilePath = filePath.toString();

            } catch (IOException e) {
                throw new RuntimeException("Error saving file: " + e.getMessage(), e);
            }
        }

        // update project details
        project.setAdditionalRequest(dto.getCustomRequest());
        project.setReferenceFilePath(savedFilePath);
        project.setStatus(ProjectStatus.RECOMMENDED); // optional: mark as "awaiting approval"
        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @Transactional
    public ProjectResponseDTO updateProjectStatus(Long projectId, ProjectStatus newStatus) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Optional: restrict invalid transitions
        if (project.getStatus() == ProjectStatus.COMPLETED && newStatus == ProjectStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a completed project.");
        }

        project.setStatus(newStatus);
        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }




}
