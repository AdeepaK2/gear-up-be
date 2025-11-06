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
import com.ead.gearup.util.TaskDTOConverter;
import com.ead.gearup.validation.RequiresRole;

import com.ead.gearup.util.ProjectDTOConverter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final AppointmentRepository appointmentRepository;
    private final VehicleRepository vehicleRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectDTOConverter projectDTOConverter;
    private final TaskDTOConverter taskDTOConverter;


    @Transactional
    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO createProject(CreateProjectDTO dto) {
        System.out.println("=== CREATE PROJECT STARTED ===");
        System.out.println("Appointment ID: " + dto.getAppointmentId());
        System.out.println("Vehicle ID: " + dto.getVehicleId());
        System.out.println("Task IDs: " + dto.getTaskIds());

        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found: " + dto.getAppointmentId()));

        // Check if a project already exists for this appointment
        if (projectRepository.existsByAppointmentAppointmentId(dto.getAppointmentId())) {
            System.out.println("Project already exists for appointment: " + dto.getAppointmentId());
            // Return the existing project instead of creating a duplicate
            Project existingProject = projectRepository.findByAppointmentAppointmentId(dto.getAppointmentId())
                    .orElseThrow(() -> new ProjectNotFoundException("Project exists but could not be retrieved"));
            System.out.println("Returning existing project ID: " + existingProject.getProjectId());
            return projectDTOConverter.convertToResponseDto(existingProject);
        }

        System.out.println("No existing project found, creating new one...");

        // Verify that the customer creating the project owns the appointment
        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (!appointment.getCustomer().getCustomerId().equals(customerId)) {
                throw new UnauthorizedAppointmentAccessException(
                        "You cannot create a project for another customer's appointment");
            }
        }

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found: " + dto.getVehicleId()));

        List<Task> tasks = taskRepository.findAllById(dto.getTaskIds());
        if (tasks.isEmpty()) {
            throw new TaskNotFoundException("No valid tasks found for project");
        }

        // Eagerly initialize customer to avoid lazy loading issues
        Customer customer = appointment.getCustomer();
        // Access customer ID to initialize the proxy
        customer.getCustomerId();

        Project project = projectDTOConverter.convertToEntity(dto);
        project.setAppointment(appointment);
        project.setVehicle(vehicle);
        project.setCustomer(customer);

        // Initialize all default values explicitly
        project.setAcceptedServicesCount(0);
        project.setTotalEstimatedCost(0.0);
        project.setTotalAcceptedCost(0.0);

        // Set a valid status - IN_PROGRESS is a safe default for new projects
        project.setStatus(ProjectStatus.IN_PROGRESS);

        // Save project first to get an ID
        System.out.println("Saving project...");
        Project savedProject = projectRepository.save(project);
        System.out.println("Project saved with ID: " + savedProject.getProjectId());

        // Set bidirectional relationship between project and tasks
        System.out.println("Linking " + tasks.size() + " tasks to project...");
        tasks.forEach(task -> {
            task.setProject(savedProject);
            task.setAssignedProject(true);
        });
        taskRepository.saveAll(tasks);
        System.out.println("Tasks saved");

        // Add tasks to project's task list
        savedProject.setTasks(tasks);
        Project finalProject = projectRepository.save(savedProject);
        System.out.println("Final project saved with " + finalProject.getTasks().size() + " tasks");
        System.out.println("=== CREATE PROJECT COMPLETED SUCCESSFULLY ===");

        return projectDTOConverter.convertToResponseDto(finalProject);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO updateProject(Long projectId, UpdateProjectDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectDTOConverter.updateEntityFromDto(dto, project);
        projectRepository.save(project);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @Transactional
    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO getProjectById(Long projectId) {
        log.info("=== GET PROJECT BY ID ===");
        log.info("Project ID: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        if (project.getVehicle() != null) {
            project.getVehicle().getVehicleId();
            project.getVehicle().getModel();
        }

        log.info("Project found: {} with {} tasks", project.getName(),
                 project.getTasks() != null ? project.getTasks().size() : 0);

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

        ProjectResponseDTO dto = projectDTOConverter.convertToResponseDto(project);
        log.info("Converted DTO with taskIds: {}", dto.getTaskIds());
        return dto;
    }

    @Transactional
    @RequiresRole({UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN})
    public List<ProjectResponseDTO> getAllProjects() {
        UserRole role = currentUserService.getCurrentUserRole();

        log.info("=== GET ALL PROJECTS ===");
        log.info("User role: {}", role);

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            log.info("Fetching projects for customer ID: {}", customerId);

            // Use optimized query with JOIN FETCH to avoid lazy loading issues
            List<Project> customerProjects = projectRepository.findAllByCustomerIdWithDetails(customerId);
            log.info("Projects found for customer: {}", customerProjects.size());

            // Initialize tasks for each project (lazy loading within transaction)
            customerProjects.forEach(project -> {
                if (project.getTasks() != null) {
                    project.getTasks().size(); // Force lazy loading to avoid LazyInitializationException
                }
            });

            List<ProjectResponseDTO> result = customerProjects.stream()
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();

            log.info("Successfully converted {} projects to DTOs", result.size());
            return result;
        }

        if (role == UserRole.EMPLOYEE) {
            Long employeeId = currentUserService.getCurrentEntityId();
            return projectRepository.findAll().stream()
                    .filter(p -> p.getAssignedEmployees().stream()
                            .anyMatch(e -> e.getEmployeeId().equals(employeeId)))
                    .map(projectDTOConverter::convertToResponseDto)
                    .toList();
        }

        // ADMIN → all projects with details
        List<Project> allProjects = projectRepository.findAllWithDetails();
        
        // Initialize tasks for each project (lazy loading within transaction)
        allProjects.forEach(project -> {
            if (project.getTasks() != null) {
                project.getTasks().size(); // Force lazy loading to avoid LazyInitializationException
            }
        });
        
        return allProjects.stream()
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
        Long employeeId = currentUserService.getCurrentEntityId();
        List<Project> projects = projectRepository.findByAssignedEmployeesEmployeeIdOrMainRepresentativeEmployeeIdWithDetails(employeeId);

        return projects.stream()
                .map(p -> {
                    EmployeeProjectResponseDTO dto = new EmployeeProjectResponseDTO();
                    dto.setProjectId(p.getProjectId());
                    dto.setProjectName(p.getName());
                    dto.setDescription(p.getDescription());
                    dto.setStatus(p.getStatus());
                    dto.setStartDate(p.getStartDate());
                    dto.setEndDate(p.getEndDate());
                    dto.setDueDate(p.getEndDate());
                    
                    if (p.getCustomer() != null) {
                        dto.setCustomerId(p.getCustomer().getCustomerId());
                        if (p.getCustomer().getUser() != null) {
                            dto.setCustomerName(p.getCustomer().getUser().getName());
                        }
                    }
                    
                    if (p.getVehicle() != null) {
                        dto.setVehicleId(p.getVehicle().getVehicleId());
                        dto.setVehicleName(p.getVehicle().getModel());
                    }
                    
                    if (p.getAppointment() != null) {
                        dto.setAppointmentId(p.getAppointment().getAppointmentId());
                    }
                    
                    if (p.getAssignedEmployees() != null) {
                        List<EmployeeProjectResponseDTO.AssignedEmployeeDTO> assignedEmployees = p.getAssignedEmployees().stream()
                                .map(emp -> {
                                    String name = emp.getUser() != null ? emp.getUser().getName() : "Unknown";
                                    return new EmployeeProjectResponseDTO.AssignedEmployeeDTO(
                                            emp.getEmployeeId(),
                                            name,
                                            emp.getSpecialization()
                                    );
                                })
                                .toList();
                        dto.setAssignedEmployees(assignedEmployees);
                    }
                    
                    if (p.getMainRepresentativeEmployee() != null) {
                        dto.setMainRepresentativeEmployeeId(p.getMainRepresentativeEmployee().getEmployeeId());
                        if (p.getMainRepresentativeEmployee().getUser() != null) {
                            dto.setMainRepresentativeEmployeeName(p.getMainRepresentativeEmployee().getUser().getName());
                        }
                        dto.setIsMainRepresentative(p.getMainRepresentativeEmployee().getEmployeeId().equals(employeeId));
                    } else {
                        dto.setIsMainRepresentative(false);
                    }
                    
                    return dto;
                })
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
    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO submitProjectReport(Long projectId, ProjectReportDTO reportDTO) {
        log.info("=== SUBMIT PROJECT REPORT ===");
        log.info("Project ID: {}, Task IDs: {}, Notes: {}", projectId, reportDTO.getTaskIds(), reportDTO.getNotes());

        Long employeeId = currentUserService.getCurrentEntityId();
        
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        boolean isAssigned = project.getAssignedEmployees().stream()
                .anyMatch(e -> e.getEmployeeId().equals(employeeId));
        boolean isMainRep = project.getMainRepresentativeEmployee() != null &&
                project.getMainRepresentativeEmployee().getEmployeeId().equals(employeeId);

        if (!isAssigned && !isMainRep) {
            throw new UnauthorizedProjectAccessException("You are not assigned to this project.");
        }

        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Project report can only be submitted for completed projects.");
        }

        if (project.getTasks() != null && !project.getTasks().isEmpty()) {
            List<Long> projectTaskIds = project.getTasks().stream()
                    .map(Task::getTaskId)
                    .toList();
            
            boolean allTasksValid = reportDTO.getTaskIds().stream()
                    .allMatch(projectTaskIds::contains);
            
            if (!allTasksValid) {
                throw new IllegalArgumentException("One or more selected tasks do not belong to this project.");
            }
        }

        String existingDescription = project.getDescription() != null ? project.getDescription() : "";
        
        // Calculate available space (255 max total)
        int maxDescriptionLength = 255;
        
        // If existing description is too long, truncate it to leave room for report
        if (existingDescription.length() > 150) {
            existingDescription = existingDescription.substring(0, 150) + "...";
        }
        
        StringBuilder reportNotes = new StringBuilder("\n\n--- Project Report ---\n");
        reportNotes.append("Submitted by: ").append(project.getMainRepresentativeEmployee() != null && 
                project.getMainRepresentativeEmployee().getEmployeeId().equals(employeeId) ? 
                "Main Representative" : "Assigned Employee").append("\n");
        reportNotes.append("Date: ").append(LocalDate.now()).append("\n");
        reportNotes.append("Completed Services: ").append(reportDTO.getTaskIds().size()).append("\n");
        
        // Calculate remaining space after header
        int usedSpace = existingDescription.length() + reportNotes.length();
        int remainingSpace = maxDescriptionLength - usedSpace;
        
        if (reportDTO.getExtraCharges() != null && !reportDTO.getExtraCharges().isEmpty()) {
            double extraChargesTotal = reportDTO.getExtraCharges().stream()
                    .mapToDouble(charge -> charge.getAmount() != null ? charge.getAmount() : 0.0)
                    .sum();
            String extraChargesLine = "Extra Charges Total: $" + String.format("%.2f", extraChargesTotal) + "\n";
            
            if (remainingSpace > extraChargesLine.length() + 20) {
                reportNotes.append(extraChargesLine);
                remainingSpace -= extraChargesLine.length();
                
                // Limit extra charges details
                int maxCharges = Math.min(3, reportDTO.getExtraCharges().size()); // Max 3 charges
                int chargeCount = 0;
                for (ProjectReportDTO.ExtraChargeDTO charge : reportDTO.getExtraCharges()) {
                    if (chargeCount >= maxCharges || remainingSpace < 30) break;
                    if (charge.getDescription() != null && !charge.getDescription().trim().isEmpty() && 
                        charge.getAmount() != null && charge.getAmount() > 0) {
                        String desc = charge.getDescription();
                        if (desc.length() > 30) {
                            desc = desc.substring(0, 27) + "...";
                        }
                        String chargeLine = "  - " + desc + ": $" + String.format("%.2f", charge.getAmount()) + "\n";
                        if (remainingSpace >= chargeLine.length()) {
                            reportNotes.append(chargeLine);
                            remainingSpace -= chargeLine.length();
                            chargeCount++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        
        // Limit notes length
        if (reportDTO.getNotes() != null && !reportDTO.getNotes().trim().isEmpty() && remainingSpace > 10) {
            String notes = reportDTO.getNotes().trim();
            int maxNotesLength = Math.min(remainingSpace - 7, 100); // Reserve 7 chars for "Notes: "
            if (notes.length() > maxNotesLength) {
                notes = notes.substring(0, maxNotesLength - 3) + "...";
            }
            reportNotes.append("Notes: ").append(notes);
        }
        
        String finalDescription = existingDescription + reportNotes.toString();
        // Final safety check - truncate if still too long
        if (finalDescription.length() > maxDescriptionLength) {
            finalDescription = finalDescription.substring(0, maxDescriptionLength - 3) + "...";
        }
        
        project.setDescription(finalDescription);
        project.setReportSentToCustomer(true);

        Project savedProject = projectRepository.save(project);
        log.info("Project report submitted successfully for project ID: {} and sent to customer", projectId);

        return projectDTOConverter.convertToResponseDto(savedProject);
    }

    @Transactional
    @RequiresRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public List<ProjectResponseDTO> getProjectsWithReportsForCurrentCustomer() {
        try {
            Long customerId = currentUserService.getCurrentEntityId();
            log.info("=== GET PROJECTS WITH REPORTS FOR CUSTOMER ===");
            log.info("Customer ID: {}", customerId);

            if (customerId == null) {
                log.warn("Customer ID is null for current user");
                throw new CustomerNotFoundException("Customer not found for current user");
            }

            List<Project> projects = projectRepository.findProjectsWithReportsByCustomerId(customerId, ProjectStatus.COMPLETED);
            log.info("Projects with reports found: {}", projects.size());

            if (projects == null || projects.isEmpty()) {
                log.info("No projects with reports found for customer ID: {}", customerId);
                return List.of();
            }

            projects.forEach(project -> {
                try {
                    if (project.getTasks() != null) {
                        project.getTasks().size();
                    }
                    if (project.getAssignedEmployees() != null) {
                        project.getAssignedEmployees().size();
                    }
                    if (project.getMainRepresentativeEmployee() != null && project.getMainRepresentativeEmployee().getUser() != null) {
                        project.getMainRepresentativeEmployee().getUser().getName();
                    }
                } catch (Exception e) {
                    log.warn("Error initializing lazy-loaded relationships for project {}: {}", 
                            project.getProjectId(), e.getMessage());
                }
            });

            List<ProjectResponseDTO> result = projects.stream()
                    .map(project -> {
                        try {
                            return projectDTOConverter.convertToResponseDto(project);
                        } catch (Exception e) {
                            log.error("Error converting project {} to DTO: {}", project.getProjectId(), e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .toList();

            log.info("Successfully converted {} projects to DTOs", result.size());
            return result;
        } catch (CustomerNotFoundException e) {
            log.error("Customer not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in getProjectsWithReportsForCurrentCustomer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve projects with reports: " + e.getMessage(), e);
        }
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
    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public ProjectResponseDTO updateProjectStatus(Long projectId, ProjectStatus newStatus) {
        log.info("=== UPDATE PROJECT STATUS ===");
        log.info("Project ID: {}, New Status: {}", projectId, newStatus);
        
        // Use repository method that eagerly loads assigned employees for validation
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        log.info("Current project status: {}", project.getStatus());
        UserRole currentRole = currentUserService.getCurrentUserRole();
        log.info("Current user role: {}", currentRole);
        
        // Restrict invalid transitions
        if (project.getStatus() == ProjectStatus.COMPLETED && newStatus == ProjectStatus.CANCELLED) {
            log.warn("Attempted to cancel a completed project: {}", projectId);
            throw new IllegalStateException("Cannot cancel a completed project.");
        }

        // Admin-only actions: Approve (CREATED -> IN_PROGRESS) and Reject (any -> CANCELLED)
        if (newStatus == ProjectStatus.IN_PROGRESS && project.getStatus() == ProjectStatus.CREATED) {
            if (currentRole != UserRole.ADMIN) {
                log.warn("Non-admin user attempted to approve project: {}", projectId);
                throw new IllegalStateException("Only admins can approve projects.");
            }
            
            // Check if at least one employee is assigned before approving
            if (project.getAssignedEmployees() == null || project.getAssignedEmployees().isEmpty()) {
                log.warn("Attempted to approve project {} without assigned employees", projectId);
                throw new IllegalStateException("Cannot approve project. At least one employee must be assigned before approval.");
            }
            
            log.info("Admin approving project: {} (CREATED -> IN_PROGRESS) with {} assigned employee(s)", 
                    projectId, project.getAssignedEmployees().size());
        }
        
        if (newStatus == ProjectStatus.CANCELLED && project.getStatus() != ProjectStatus.COMPLETED) {
            if (currentRole != UserRole.ADMIN) {
                log.warn("Non-admin user attempted to cancel project: {}", projectId);
                throw new IllegalStateException("Only admins can reject/cancel projects.");
            }
            log.info("Admin rejecting/cancelling project: {} ({} -> CANCELLED)", projectId, project.getStatus());
        }

        ProjectStatus oldStatus = project.getStatus();
        project.setStatus(newStatus);
        projectRepository.save(project);
        
        log.info("Project status updated successfully: {} ({} -> {})", projectId, oldStatus, newStatus);

        return projectDTOConverter.convertToResponseDto(project);
    }

    @Transactional
    @RequiresRole({UserRole.ADMIN})
    public ProjectResponseDTO assignEmployees(Long projectId, List<Long> employeeIds, Long mainRepresentativeEmployeeId) {
        log.info("=== ASSIGN EMPLOYEES TO PROJECT ===");
        log.info("Project ID: {}, Employee IDs: {}, Main Representative ID: {}", projectId, employeeIds, mainRepresentativeEmployeeId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Fetch employees by IDs
        List<Employee> employees = employeeRepository.findAllById(employeeIds);

        if (employees.isEmpty()) {
            throw new ResourceNotFoundException("No valid employees found for the provided IDs");
        }

        log.info("Found {} employees to assign", employees.size());
        log.info("Employee IDs to assign: {}", employees.stream().map(Employee::getEmployeeId).toList());

        // Clear existing assignments first to ensure clean state
        project.getAssignedEmployees().clear();
        log.info("Cleared existing employee assignments");

        // Add all employees to the assigned employees list
        project.getAssignedEmployees().addAll(employees);
        log.info("Added {} employees to assigned employees list", employees.size());
        log.info("Assigned employee IDs: {}", project.getAssignedEmployees().stream().map(Employee::getEmployeeId).toList());

        // Validate and set main representative
        Employee mainRepresentative = null;
        if (mainRepresentativeEmployeeId != null) {
            if (!employeeIds.contains(mainRepresentativeEmployeeId)) {
                throw new IllegalArgumentException("Main representative employee must be one of the assigned employees");
            }
            mainRepresentative = employeeRepository.findById(mainRepresentativeEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Main representative employee not found: " + mainRepresentativeEmployeeId));
            
            // Ensure main representative is in the assigned employees list
            // Use final variable for lambda expression
            final Long mainRepId = mainRepresentative.getEmployeeId();
            boolean mainRepInList = project.getAssignedEmployees().stream()
                    .anyMatch(e -> e.getEmployeeId().equals(mainRepId));
            if (!mainRepInList) {
                log.warn("Main representative (ID: {}) not in assigned employees list, adding it now", mainRepresentativeEmployeeId);
                project.getAssignedEmployees().add(mainRepresentative);
                log.info("Main representative added to assigned employees list");
            } else {
                log.info("Main representative (ID: {}) confirmed in assigned employees list", mainRepresentativeEmployeeId);
            }
            
            project.setMainRepresentativeEmployee(mainRepresentative);
            log.info("Main representative set to employee ID: {} (Name: {})", 
                    mainRepresentativeEmployeeId, 
                    mainRepresentative.getUser() != null ? mainRepresentative.getUser().getName() : "Unknown");
        } else {
            // If multiple employees but no main representative specified, set the first one as default
            if (employees.size() > 1) {
                log.warn("Multiple employees assigned but no main representative specified. Setting first employee as main representative.");
                mainRepresentative = employees.get(0);
                project.setMainRepresentativeEmployee(mainRepresentative);
            } else if (employees.size() == 1) {
                // Single employee automatically becomes the main representative
                mainRepresentative = employees.get(0);
                project.setMainRepresentativeEmployee(mainRepresentative);
                log.info("Single employee automatically set as main representative: ID {}", mainRepresentative.getEmployeeId());
            } else {
                project.setMainRepresentativeEmployee(null);
                log.info("No employees assigned, main representative set to null");
            }
        }

        // IMPORTANT: Do NOT change project status here - status should remain as CREATED
        // Status will only change when admin explicitly approves via updateProjectStatus()
        log.info("Project status before save: {} (will remain unchanged)", project.getStatus());
        
        // Save the project with all relationships (status remains unchanged)
        Project savedProject = projectRepository.save(project);
        log.info("Project saved with ID: {} (status remains: {})", savedProject.getProjectId(), savedProject.getStatus());
        
        // Flush to ensure data is written to database immediately
        projectRepository.flush();
        
        // Verify what was saved by reloading from database (ensures we get fresh data from DB)
        // Note: We don't fetch tasks to avoid MultipleBagFetchException
        Project verifiedProject = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found after save: " + projectId));
        
        // Initialize tasks lazily if needed (they will be loaded when accessed)
        // Accessing tasks here ensures they're loaded within the transaction
        if (verifiedProject.getTasks() != null) {
            verifiedProject.getTasks().size(); // Force lazy loading
        }
        
        log.info("=== VERIFICATION OF SAVED DATA ===");
        log.info("Assigned employees count: {}", verifiedProject.getAssignedEmployees().size());
        log.info("Assigned employee IDs: {}", verifiedProject.getAssignedEmployees().stream()
                .map(Employee::getEmployeeId)
                .toList());
        
        if (verifiedProject.getMainRepresentativeEmployee() != null) {
            Long mainRepId = verifiedProject.getMainRepresentativeEmployee().getEmployeeId();
            log.info("Main representative ID: {}", mainRepId);
            
            boolean mainRepInAssignedList = verifiedProject.getAssignedEmployees().stream()
                    .anyMatch(e -> e.getEmployeeId().equals(mainRepId));
            log.info("Main representative in assigned employees list: {}", mainRepInAssignedList);
            
            if (!mainRepInAssignedList) {
                log.error("ERROR: Main representative (ID: {}) is NOT in the assigned employees list!", mainRepId);
                throw new IllegalStateException("Main representative must be in the assigned employees list");
            }
        } else {
            log.warn("WARNING: Main representative is NULL after save!");
            if (!verifiedProject.getAssignedEmployees().isEmpty()) {
                log.error("ERROR: Employees were assigned but main representative is NULL!");
            }
        }
        
        log.info("=== VERIFICATION COMPLETE ===");
        log.info("All employees (including main representative) saved successfully to project {}", projectId);

        return projectDTOConverter.convertToResponseDto(verifiedProject);
    }



}
