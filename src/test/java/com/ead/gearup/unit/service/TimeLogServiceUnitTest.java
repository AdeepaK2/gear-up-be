package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
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
import com.ead.gearup.exception.ExceededEstimatedHoursException;
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
        testTask.setEstimatedHours(10); // 10 hours estimated

        // Setup test project with tasks
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setTasks(Arrays.asList(testTask));

        // Setup test time log
        testTimeLog = new TimeLog();
        testTimeLog.setLogId(1L);
        testTimeLog.setEmployee(testEmployee);
        testTimeLog.setTask(testTask);
        testTimeLog.setProject(testProject);
        testTimeLog.setStartTime(LocalDateTime.now());
        testTimeLog.setEndTime(LocalDateTime.now().plusHours(2));
        testTimeLog.setHoursWorked(2.0);
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
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(5.0); // 5 hours already logged
        when(converter.convertToEntity(any(), any(), any(), any(), any())).thenReturn(testTimeLog);
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

    // ========== Estimated Hours Validation Tests ==========
    @Test
    void testCreateTimeLog_ExceedsEstimatedHours() {
        // Arrange - Project has 10 hours estimated, 9 hours already logged, trying to log 2 more hours
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(9.0);

        // Act & Assert
        ExceededEstimatedHoursException exception = assertThrows(
            ExceededEstimatedHoursException.class,
            () -> timeLogService.createTimeLog(createDTO)
        );
        
        assertTrue(exception.getMessage().contains("exceed estimated hours"));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testCreateTimeLog_ExactlyAtEstimatedHours() {
        // Arrange - Project has 10 hours estimated, 8 hours already logged, trying to log 2 more hours (total = 10)
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(8.0);
        when(converter.convertToEntity(any(), any(), any(), any(), any())).thenReturn(testTimeLog);
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(testTimeLog);
        when(converter.convertToResponseDTO(any())).thenReturn(responseDTO);

        // Act
        TimeLogResponseDTO result = timeLogService.createTimeLog(createDTO);

        // Assert - Should succeed as it's exactly at the limit
        assertNotNull(result);
        verify(timeLogRepository, times(1)).save(any(TimeLog.class));
    }

    @Test
    void testUpdateTimeLog_ExceedsEstimatedHours() {
        // Arrange
        testTimeLog.setHoursWorked(2.0);
        when(timeLogRepository.findById(1L)).thenReturn(Optional.of(testTimeLog));
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(9.0); // 9 hours total (including current 2)
        
        UpdateTimeLogDTO updateDTO = new UpdateTimeLogDTO();
        updateDTO.setStartTime(LocalDateTime.now());
        updateDTO.setEndTime(LocalDateTime.now().plusHours(5)); // Trying to change to 5 hours

        // Act & Assert
        ExceededEstimatedHoursException exception = assertThrows(
            ExceededEstimatedHoursException.class,
            () -> timeLogService.updateTimeLog(1L, updateDTO)
        );
        
        assertTrue(exception.getMessage().contains("exceed estimated hours"));
        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void testGetProjectTimeLogSummary_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(6.5);

        // Act
        ProjectTimeLogSummaryDTO summary = timeLogService.getProjectTimeLogSummary(1L);

        // Assert
        assertNotNull(summary);
        assertEquals(1L, summary.getProjectId());
        assertEquals("Test Project", summary.getProjectName());
        assertEquals(10, summary.getTotalEstimatedHours());
        assertEquals(6.5, summary.getTotalLoggedHours());
        assertEquals(3.5, summary.getRemainingHours());
        assertEquals(65.0, summary.getPercentageUsed());
        assertFalse(summary.getIsOverBudget());
    }

    @Test
    void testGetProjectTimeLogSummary_OverBudget() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(timeLogRepository.getTotalLoggedHoursByProjectId(1L)).thenReturn(12.0);

        // Act
        ProjectTimeLogSummaryDTO summary = timeLogService.getProjectTimeLogSummary(1L);

        // Assert
        assertNotNull(summary);
        assertEquals(1L, summary.getProjectId());
        assertEquals(10, summary.getTotalEstimatedHours());
        assertEquals(12.0, summary.getTotalLoggedHours());
        assertTrue(summary.getIsOverBudget());
    }
}
