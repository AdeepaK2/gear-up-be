package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ead.gearup.dto.task.EmployeeRecentActivityDTO;
import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskUpdateDTO;
import com.ead.gearup.enums.TaskStatus;
import com.ead.gearup.exception.TaskNotFoundException;
import com.ead.gearup.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private TaskResponseDTO testTaskResponse;
    private TaskCreateDTO testTaskCreate;
    private TaskUpdateDTO testTaskUpdate;

    @BeforeEach
    void setUp() {
        testTaskResponse = new TaskResponseDTO();
        testTaskResponse.setTaskId(1L);
        testTaskResponse.setName("Oil Change");
        testTaskResponse.setStatus(TaskStatus.PENDING);

        testTaskCreate = new TaskCreateDTO();
        testTaskCreate.setName("Oil Change");
        testTaskCreate.setAppointmentId(1L);

        testTaskUpdate = new TaskUpdateDTO();
        testTaskUpdate.setName("Updated Task Name");
    }

    // ========== POST /api/v1/tasks ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateTask_Success() throws Exception {
        // Arrange
        when(taskService.createTask(any(TaskCreateDTO.class)))
                .thenReturn(testTaskResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTaskCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task created successfully"))
                .andExpect(jsonPath("$.data.taskId").value(1))
                .andExpect(jsonPath("$.data.name").value("Oil Change"));

        verify(taskService, times(1)).createTask(any(TaskCreateDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateTask_InvalidData() throws Exception {
        // Arrange
        when(taskService.createTask(any(TaskCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid task data"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTaskCreate)))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/tasks/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTaskById_Success() throws Exception {
        // Arrange
        when(taskService.getTaskById(1L)).thenReturn(testTaskResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task fetched successfully"))
                .andExpect(jsonPath("$.data.taskId").value(1));

        verify(taskService, times(1)).getTaskById(1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTaskById_NotFound() throws Exception {
        // Arrange
        when(taskService.getTaskById(999L))
                .thenThrow(new TaskNotFoundException("Task not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/tasks ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAllTasks_Success() throws Exception {
        // Arrange
        List<TaskResponseDTO> tasks = Arrays.asList(testTaskResponse);
        when(taskService.getAllTasks()).thenReturn(tasks);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].taskId").value(1));

        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAllTasks_EmptyList() throws Exception {
        // Arrange
        when(taskService.getAllTasks()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== PATCH /api/v1/tasks/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateTask_Success() throws Exception {
        // Arrange
        TaskResponseDTO updatedResponse = new TaskResponseDTO();
        updatedResponse.setTaskId(1L);
        updatedResponse.setName("Updated Task Name");

        when(taskService.updateTask(anyLong(), any(TaskUpdateDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTaskUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Task Name"));

        verify(taskService, times(1)).updateTask(anyLong(), any(TaskUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateTask_NotFound() throws Exception {
        // Arrange
        when(taskService.updateTask(anyLong(), any(TaskUpdateDTO.class)))
                .thenThrow(new TaskNotFoundException("Task not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTaskUpdate)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /api/v1/tasks/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDeleteTask_Success() throws Exception {
        // Arrange
        doNothing().when(taskService).deleteTask(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Task deleted successfully"));

        verify(taskService, times(1)).deleteTask(1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDeleteTask_NotFound() throws Exception {
        // Arrange
        doThrow(new TaskNotFoundException("Task not found"))
                .when(taskService).deleteTask(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTask_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isUnauthorized());

        verify(taskService, never()).deleteTask(anyLong());
    }

    // ========== GET /api/v1/tasks/employee/recent-activities ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetRecentActivitiesForCurrentEmployee_Success() throws Exception {
        // Arrange
        EmployeeRecentActivityDTO activity = new EmployeeRecentActivityDTO();
        List<EmployeeRecentActivityDTO> activities = Arrays.asList(activity);

        when(taskService.getRecentActivitiesForCurrentEmployee()).thenReturn(activities);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/employee/recent-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Recent activities fetched successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(taskService, times(1)).getRecentActivitiesForCurrentEmployee();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetRecentActivitiesForCurrentEmployee_EmptyList() throws Exception {
        // Arrange
        when(taskService.getRecentActivitiesForCurrentEmployee()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/employee/recent-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
