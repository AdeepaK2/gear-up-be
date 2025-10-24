package com.ead.gearup.service;

import com.ead.gearup.dto.task.TaskUpdateDTO;
import com.ead.gearup.enums.TaskStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AppointmentNotFoundException;
import com.ead.gearup.exception.TaskNotFoundException;
import com.ead.gearup.exception.UnauthorizedTaskAccessException;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.validation.RequiresRole;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.task.EmployeeRecentActivityDTO;
import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskSearchResponseDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.repository.TaskRepository;
import com.ead.gearup.util.TaskDTOConverter;
import com.ead.gearup.model.Employee;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.repository.EmployeeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {

    private final TaskDTOConverter taskDTOConverter;
    private final TaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;
    private final EmployeeRepository employeeRepository;

    @RequiresRole({ UserRole.EMPLOYEE, UserRole.ADMIN })
    public TaskResponseDTO createTask(TaskCreateDTO taskCreateDTO) {

        Appointment appointment = appointmentRepository.findById(taskCreateDTO.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found" + taskCreateDTO.getAppointmentId()));

        Task task = taskDTOConverter.convertToEntity(taskCreateDTO);
        task.setAppointment(appointment);
        taskRepository.save(task);

        return taskDTOConverter.convertToResponseDto(task);
    }

    @RequiresRole({ UserRole.EMPLOYEE, UserRole.ADMIN, UserRole.CUSTOMER })
    public TaskResponseDTO getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found " + taskId));

        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();
            if (task.getProject() == null ||
                    !task.getProject().getCustomer().getCustomerId().equals(customerId)) {
                throw new TaskNotFoundException("Task not found " + taskId);
            }
        }

        // if(role == UserRole.EMPLOYEE) {
        // Long employeeId = currentUserService.getCurrentEntityId();
        // boolean assigned = task.getProject()!=null &&
        // task.getProject().getAssignedEmployees().stream()
        // .anyMatch(employee -> employee.getEmployeeId().equals(employeeId));
        // if(!assigned) {
        // throw new TaskNotFoundException("Task not found "+ taskId);
        // }
        // }

        return taskDTOConverter.convertToResponseDto(task);
    }

    @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public List<TaskResponseDTO> getAllTasks() {
        UserRole role = currentUserService.getCurrentUserRole();

        if (role == UserRole.CUSTOMER) {
            Long customerId = currentUserService.getCurrentEntityId();

            return taskRepository.findAll().stream()
                    .filter(t -> t.getProject() != null
                            && t.getProject().getCustomer().getCustomerId().equals(customerId))
                    .map(taskDTOConverter::convertToResponseDto)
                    .toList();
        }

        // if(role == UserRole.EMPLOYEE) {
        // Long employeeId = currentUserService.getCurrentEntityId();
        //
        // return taskRepository.findAll().stream()
        // .filter(t->t.getProject() != null
        // && t.getProject().getAssignedEmployees().stream()
        // .anyMatch(e->e.getEmployeeId().equals(employeeId)))
        // .map(taskDTOConverter::convertToResponseDto)
        // .toList();
        // }

        return taskRepository.findAll().stream()
                .map(taskDTOConverter::convertToResponseDto)
                .toList();
    }

    @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public TaskResponseDTO updateTask(Long taskId, TaskUpdateDTO taskUpdateDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found " + taskId));

        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.CUSTOMER) {
            if (taskUpdateDTO.getStatus() == null) {
                throw new UnauthorizedTaskAccessException("Customers can only update task approval status.");
            }
            task.setStatus(taskUpdateDTO.getStatus());
        }

        Task updatedTask = taskDTOConverter.updateEntityFromDto(task, taskUpdateDTO);
        taskRepository.save(updatedTask);

        return taskDTOConverter.convertToResponseDto(updatedTask);
    }

    @RequiresRole(UserRole.ADMIN)
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found " + taskId));

        UserRole role = currentUserService.getCurrentUserRole();

        if (role != UserRole.ADMIN) {
            throw new UnauthorizedTaskAccessException("Only admins can delete task.");
        }

        taskRepository.delete(task);
    }

    @RequiresRole({UserRole.EMPLOYEE, UserRole.ADMIN})
    public Map<String, Long> getTaskSummaryForEmployee() {
        Long employeeId = currentUserService.getCurrentEntityId();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new UserNotFoundException("Employee not found with ID: " + employeeId));

        Long assigned = taskRepository.countByEmployeeAndStatus(employee, TaskStatus.CONFIRMED);
        Long inProgress = taskRepository.countByEmployeeAndStatus(employee, TaskStatus.IN_PROGRESS);
        Long completedToday = taskRepository.countCompletedToday(employee);

        return Map.of(
            "assigned", assigned,
            "inprogress", inProgress,
            "completedToday", completedToday
        );


    }
    
    @RequiresRole({UserRole.EMPLOYEE})
    public List<EmployeeRecentActivityDTO> getRecentActivitiesForCurrentEmployee(){
        Long employeeId = currentUserService.getCurrentEntityId();

        //Get all tasks for this employee
        List<Task> tasks = employeeRepository.findById(employeeId)
                .orElseThrow(()-> new UserNotFoundException("Employee not found with ID: "+ employeeId))
                .getTasks();

        // Filter in-progress tasks
        List<String> taskNames = tasks.stream()
                .filter(t->t.getStatus() == TaskStatus.IN_PROGRESS)
                .map(Task::getName)
                .toList();

        return taskNames.stream()
                .map(name -> new EmployeeRecentActivityDTO(name))
                .toList();
    }

    public List<TaskSearchResponseDTO> searchTasksByTaskName(String name) {
        return taskRepository.findTaskSearchResultsNative(name)
                .stream()
                .map(task -> new TaskSearchResponseDTO(
                        task.getTaskId(),
                        task.getName(),
                        task.getDescription(),
                        task.getEstimatedHours(),
                        task.getCost(),
                        task.getStatus(),
                        task.isAssignedProject()))
                .collect(Collectors.toList());
    }

}
