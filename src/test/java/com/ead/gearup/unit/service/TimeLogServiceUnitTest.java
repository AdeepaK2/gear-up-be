package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.timelog.*;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.*;
import com.ead.gearup.service.TimeLogService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.TimeLogDTOConverter;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceUnitTest {

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private TimeLogDTOConverter converter;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TimeLogService timeLogService;

    private TimeLog testTimeLog;
    private Employee testEmployee;
    private Task testTask;
    private Project testProject;
    private CreateTimeLogDTO createDTO;
    private UpdateTimeLogDTO updateDTO;
    private TimeLogResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Setup test employee
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1L);

        // Setup test task
        testTask = new Task();
        testTask.setTaskId(1L);

        // Setup test project
        testProject = new Project();
        testProject.setProjectId(1L);

        // Setup test time log
        testTimeLog = new TimeLog();
        testTimeLog.setLogId(1L);
        testTimeLog.setEmployee(testEmployee);
        testTimeLog.setTask(testTask);
        testTimeLog.setProject(testProject);
        testTimeLog.setStartTime(LocalDateTime.now());
        testTimeLog.setEndTime(LocalDateTime.now().plusHours(2));
        testTimeLog.setDescription("Test time log");

        // Setup DTOs
        createDTO = new CreateTimeLogDTO();
        createDTO.setTaskId(1L);
        createDTO.setProjectId(1L);
        createDTO.setStartTime(LocalDateTime.now());
        createDTO.setEndTime(LocalDateTime.now().plusHours(2));
        createDTO.setDescription("Test time log");

        updateDTO = new UpdateTimeLogDTO();
        updateDTO.setDescription("Updated description");

        responseDTO = new TimeLogResponseDTO();
        responseDTO.setLogId(1L);
        responseDTO.setDescription("Test time log");
    }

    // ========== createTimeLog() Tests ==========
    @Test
    void testCreateTimeLog_Success() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(converter.convertToEntity(any(), any(), any(), any())).thenReturn(testTimeLog);
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(testTimeLog);
        when(converter.convertToResponseDTO(any())).thenReturn(responseDTO);

        // Act
        TimeLogResponseDTO result = timeLogService.createTimeLog(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test time log", result.getDescription());
        verify(timeLogRepository, times(1)).save(any(TimeLog.class));
    }

    @Test
    void testCreateTimeLog_EmployeeNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EmployeeNotFoundException.class, () -> timeLogService.createTimeLog(createDTO));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testCreateTimeLog_TaskNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        createDTO.setTaskId(999L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> timeLogService.createTimeLog(createDTO));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testCreateTimeLog_ProjectNotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        createDTO.setProjectId(999L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> timeLogService.createTimeLog(createDTO));
        verify(timeLogRepository, never()).save(any());
    }

    // ========== getTimeLogById() Tests ==========
    @Test
    void testGetTimeLogById_Success() {
        // Arrange
        when(timeLogRepository.findById(1L)).thenReturn(Optional.of(testTimeLog));
        when(converter.convertToResponseDTO(any())).thenReturn(responseDTO);

        // Act
        TimeLogResponseDTO result = timeLogService.getTimeLogById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getLogId());
        verify(timeLogRepository, times(1)).findById(1L);
    }

    @Test
    void testGetTimeLogById_NotFound() {
        // Arrange
        when(timeLogRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> timeLogService.getTimeLogById(999L));
    }

    // ========== getAllTimeLogs() Tests ==========
    @Test
    void testGetAllTimeLogs_Success() {
        // Arrange
        List<TimeLog> timeLogs = Arrays.asList(testTimeLog);
        when(timeLogRepository.findAll()).thenReturn(timeLogs);
        when(converter.convertToResponseDTO(any())).thenReturn(responseDTO);

        // Act
        List<TimeLogResponseDTO> result = timeLogService.getAllTimeLogs();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(timeLogRepository, times(1)).findAll();
    }

    @Test
    void testGetAllTimeLogs_EmptyList() {
        // Arrange
        when(timeLogRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<TimeLogResponseDTO> result = timeLogService.getAllTimeLogs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== updateTimeLog() Tests ==========
    @Test
    void testUpdateTimeLog_Success() {
        // Arrange
        when(timeLogRepository.findById(1L)).thenReturn(Optional.of(testTimeLog));
        doNothing().when(converter).updateEntityFromDTO(any(), any());
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(testTimeLog);
        when(converter.convertToResponseDTO(any())).thenReturn(responseDTO);

        // Act
        TimeLogResponseDTO result = timeLogService.updateTimeLog(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(timeLogRepository, times(1)).save(testTimeLog);
        verify(converter, times(1)).updateEntityFromDTO(testTimeLog, updateDTO);
    }

    @Test
    void testUpdateTimeLog_NotFound() {
        // Arrange
        when(timeLogRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> timeLogService.updateTimeLog(999L, updateDTO));
        verify(timeLogRepository, never()).save(any());
    }

    // ========== deleteTimeLog() Tests ==========
    @Test
    void testDeleteTimeLog_Success() {
        // Arrange
        when(timeLogRepository.existsById(1L)).thenReturn(true);
        doNothing().when(timeLogRepository).deleteById(1L);

        // Act
        timeLogService.deleteTimeLog(1L);

        // Assert
        verify(timeLogRepository, times(1)).existsById(1L);
        verify(timeLogRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteTimeLog_NotFound() {
        // Arrange
        when(timeLogRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> timeLogService.deleteTimeLog(999L));
        verify(timeLogRepository, never()).deleteById(any());
    }
}
