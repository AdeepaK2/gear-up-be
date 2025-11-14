package com.ead.gearup.service;

import com.ead.gearup.dto.timelog.*;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.exception.ExceededEstimatedHoursException;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.Task;
import com.ead.gearup.model.TimeLog;
import com.ead.gearup.model.Project;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.ProjectRepository;
import com.ead.gearup.repository.TaskRepository;
import com.ead.gearup.repository.TimeLogRepository;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.TimeLogDTOConverter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final TimeLogDTOConverter converter;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    public TimeLogService(TimeLogRepository timeLogRepository,
            TimeLogDTOConverter converter,
            EmployeeRepository employeeRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            AppointmentRepository appointmentRepository,
            CurrentUserService currentUserService) {
        this.timeLogRepository = timeLogRepository;
        this.converter = converter;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
    }

    public TimeLogResponseDTO createTimeLog(CreateTimeLogDTO dto) {

        Employee employee = employeeRepository.findById(currentUserService.getCurrentEntityId())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with id " + currentUserService.getCurrentEntityId()));

        Task task = null;
        Project project = null;
        Appointment appointment = null;

        // Handle appointment-based time log
        if (dto.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(dto.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Appointment not found with id " + dto.getAppointmentId()));
            
            // No validation needed for appointments - they don't have estimated hours
            TimeLog timeLog = converter.convertToEntity(dto, employee, null, null, appointment);
            TimeLog saved = timeLogRepository.save(timeLog);
            return converter.convertToResponseDTO(saved);
        }

        // Handle project/task-based time log (existing logic)
        task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + dto.getTaskId()));

        project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + dto.getProjectId()));

        // Calculate hours for the new time log entry
        double newLogHours = calculateHours(dto.getStartTime(), dto.getEndTime());
        
        // Get total logged hours for the project so far
        Double totalLoggedHours = timeLogRepository.getTotalLoggedHoursByProjectId(project.getProjectId());
        
        // Calculate total estimated hours for all services/tasks in the project
        int totalEstimatedHours = project.getTasks().stream()
                .mapToInt(Task::getEstimatedHours)
                .sum();
        
        // Check if adding this time log would exceed the estimated hours
        double totalAfterNewLog = totalLoggedHours + newLogHours;
        if (totalAfterNewLog > totalEstimatedHours) {
            throw new ExceededEstimatedHoursException(
                String.format(
                    "Cannot log %.2f hours. Total logged hours (%.2f) would exceed estimated hours (%d). Remaining: %.2f hours.",
                    newLogHours,
                    totalAfterNewLog,
                    totalEstimatedHours,
                    totalEstimatedHours - totalLoggedHours
                )
            );
        }

        TimeLog timeLog = converter.convertToEntity(dto, employee, task, project, null);
        TimeLog saved = timeLogRepository.save(timeLog);
        return converter.convertToResponseDTO(saved);
    }

    private double calculateHours(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }
        long seconds = Duration.between(startTime, endTime).getSeconds();
        return Math.round((seconds / 3600.0) * 100.0) / 100.0;
    }

    public TimeLogResponseDTO getTimeLogById(Long id) {
        TimeLog timeLog = timeLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
        return converter.convertToResponseDTO(timeLog);
    }

    public List<TimeLogResponseDTO> getAllTimeLogs() {
        return timeLogRepository.findAll().stream()
                .map(converter::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public TimeLogResponseDTO updateTimeLog(Long id, UpdateTimeLogDTO dto) {
        TimeLog timeLog = timeLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeLog not found with id " + id));
        
        // If start time or end time is being updated, validate against estimated hours
        if (dto.getStartTime() != null || dto.getEndTime() != null) {
            java.time.LocalDateTime newStartTime = dto.getStartTime() != null ? dto.getStartTime() : timeLog.getStartTime();
            java.time.LocalDateTime newEndTime = dto.getEndTime() != null ? dto.getEndTime() : timeLog.getEndTime();
            
            double newLogHours = calculateHours(newStartTime, newEndTime);
            double oldLogHours = timeLog.getHoursWorked();
            
            // Get total logged hours for the project (excluding this time log)
            Double totalLoggedHours = timeLogRepository.getTotalLoggedHoursByProjectId(timeLog.getProject().getProjectId());
            totalLoggedHours -= oldLogHours; // Subtract the old hours
            
            // Calculate total estimated hours
            int totalEstimatedHours = timeLog.getProject().getTasks().stream()
                    .mapToInt(Task::getEstimatedHours)
                    .sum();
            
            // Check if updating would exceed estimated hours
            double totalAfterUpdate = totalLoggedHours + newLogHours;
            if (totalAfterUpdate > totalEstimatedHours) {
                throw new ExceededEstimatedHoursException(
                    String.format(
                        "Cannot update to %.2f hours. Total logged hours (%.2f) would exceed estimated hours (%d). Remaining: %.2f hours.",
                        newLogHours,
                        totalAfterUpdate,
                        totalEstimatedHours,
                        totalEstimatedHours - totalLoggedHours
                    )
                );
            }
        }
        
        converter.updateEntityFromDTO(timeLog, dto);
        return converter.convertToResponseDTO(timeLogRepository.save(timeLog));
    }

    public void deleteTimeLog(Long id) {
        if (!timeLogRepository.existsById(id)) {
            throw new ResourceNotFoundException("TimeLog not found with id " + id);
        }
        timeLogRepository.deleteById(id);
    }

    public ProjectTimeLogSummaryDTO getProjectTimeLogSummary(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        // Calculate total estimated hours from all tasks in the project
        int totalEstimatedHours = project.getTasks().stream()
                .mapToInt(Task::getEstimatedHours)
                .sum();

        // Get total logged hours
        Double totalLoggedHours = timeLogRepository.getTotalLoggedHoursByProjectId(projectId);

        // Calculate remaining hours
        double remainingHours = totalEstimatedHours - totalLoggedHours;

        // Calculate percentage used
        double percentageUsed = totalEstimatedHours > 0 
            ? (totalLoggedHours / totalEstimatedHours) * 100 
            : 0.0;

        // Check if over budget
        boolean isOverBudget = totalLoggedHours > totalEstimatedHours;

        return ProjectTimeLogSummaryDTO.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalEstimatedHours(totalEstimatedHours)
                .totalLoggedHours(Math.round(totalLoggedHours * 100.0) / 100.0)
                .remainingHours(Math.round(remainingHours * 100.0) / 100.0)
                .percentageUsed(Math.round(percentageUsed * 100.0) / 100.0)
                .isOverBudget(isOverBudget)
                .build();
    }

    public List<TimeLogResponseDTO> getTimeLogsByAppointment(Long appointmentId) {
        // Verify appointment exists
        appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));

        // Get all time logs for this appointment
        return timeLogRepository.findAll().stream()
                .filter(log -> log.getAppointment() != null && log.getAppointment().getAppointmentId().equals(appointmentId))
                .map(converter::convertToResponseDTO)
                .collect(Collectors.toList());
    }
}
