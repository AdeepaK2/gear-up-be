package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskUpdateDTO;
import com.ead.gearup.enums.TaskStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.*;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.*;
import com.ead.gearup.service.TaskService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.TaskDTOConverter;

@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTest {

    @Mock
    private TaskDTOConverter taskDTOConverter;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private Appointment testAppointment;
    private Project testProject;
    private Customer testCustomer;
    private TaskCreateDTO createDTO;
    private TaskUpdateDTO updateDTO;
    private TaskResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);

        // Setup test appointment
        testAppointment = new Appointment();
        testAppointment.setAppointmentId(1L);

        // Setup test project
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setCustomer(testCustomer);

        // Setup test task
        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setName("Oil Change");
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setAppointment(testAppointment);
        testTask.setProject(testProject);

        // Setup DTOs
        createDTO = new TaskCreateDTO();
        createDTO.setAppointmentId(1L);
        createDTO.setName("Oil Change");

        updateDTO = new TaskUpdateDTO();
        updateDTO.setName("Updated Task");

        responseDTO = new TaskResponseDTO();
        responseDTO.setTaskId(1L);
        responseDTO.setName("Oil Change");
    }

    // ========== createTask() Tests ==========
    @Test
    void testCreateTask_Success() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(taskDTOConverter.convertToEntity(any())).thenReturn(testTask);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        TaskResponseDTO result = taskService.createTask(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Oil Change", result.getName());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testCreateTask_AppointmentNotFound() {
        // Arrange
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());
        createDTO.setAppointmentId(999L);

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class, () -> taskService.createTask(createDTO));
        verify(taskRepository, never()).save(any());
    }

    // ========== getTaskById() Tests ==========
    @Test
    void testGetTaskById_Success_AsAdmin() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        TaskResponseDTO result = taskService.getTaskById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTaskId());
    }

    @Test
    void testGetTaskById_Success_AsCustomer() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        TaskResponseDTO result = taskService.getTaskById(1L);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetTaskById_UnauthorizedCustomer() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(999L); // Different customer

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(1L));
    }

    @Test
    void testGetTaskById_NotFound() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(999L));
    }

    // ========== getAllTasks() Tests ==========
    @Test
    void testGetAllTasks_AsAdmin() {
        // Arrange
        List<Task> tasks = Arrays.asList(testTask);
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(taskRepository.findAll()).thenReturn(tasks);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        List<TaskResponseDTO> result = taskService.getAllTasks();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllTasks_AsCustomer() {
        // Arrange
        List<Task> tasks = Arrays.asList(testTask);
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(taskRepository.findAll()).thenReturn(tasks);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        List<TaskResponseDTO> result = taskService.getAllTasks();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== updateTask() Tests ==========
    @Test
    void testUpdateTask_Success_AsAdmin() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(taskDTOConverter.updateEntityFromDto(any(), any())).thenReturn(testTask);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        TaskResponseDTO result = taskService.updateTask(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void testUpdateTask_AsCustomer_OnlyStatusUpdate() {
        // Arrange
        TaskUpdateDTO statusUpdateDTO = new TaskUpdateDTO();
        statusUpdateDTO.setStatus(TaskStatus.ACCEPTED);
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(taskDTOConverter.updateEntityFromDto(any(), any())).thenReturn(testTask);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        TaskResponseDTO result = taskService.updateTask(1L, statusUpdateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(TaskStatus.ACCEPTED, testTask.getStatus());
    }

    @Test
    void testUpdateTask_AsCustomer_InvalidUpdate() {
        // Arrange
        TaskUpdateDTO invalidUpdateDTO = new TaskUpdateDTO();
        invalidUpdateDTO.setName("New Name"); // Customer can't update name
        invalidUpdateDTO.setStatus(null);
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);

        // Act & Assert
        assertThrows(UnauthorizedTaskAccessException.class, 
            () -> taskService.updateTask(1L, invalidUpdateDTO));
    }

    @Test
    void testUpdateTask_NotFound() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(999L, updateDTO));
    }

    // ========== deleteTask() Tests ==========
    @Test
    void testDeleteTask_Success_AsAdmin() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        doNothing().when(taskRepository).delete(testTask);

        // Act
        taskService.deleteTask(1L);

        // Assert
        verify(taskRepository, times(1)).delete(testTask);
    }

    @Test
    void testDeleteTask_Unauthorized_AsEmployee() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.EMPLOYEE);

        // Act & Assert
        assertThrows(UnauthorizedTaskAccessException.class, () -> taskService.deleteTask(1L));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void testDeleteTask_NotFound() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(999L));
    }
}
