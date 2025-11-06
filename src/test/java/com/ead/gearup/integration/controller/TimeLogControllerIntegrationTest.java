package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
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

import com.ead.gearup.dto.timelog.CreateTimeLogDTO;
import com.ead.gearup.dto.timelog.TimeLogResponseDTO;
import com.ead.gearup.dto.timelog.UpdateTimeLogDTO;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.service.TimeLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class TimeLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimeLogService timeLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private TimeLogResponseDTO testTimeLogResponse;
    private CreateTimeLogDTO testTimeLogCreate;
    private UpdateTimeLogDTO testTimeLogUpdate;

    @BeforeEach
    void setUp() {
        testTimeLogResponse = new TimeLogResponseDTO();
        testTimeLogResponse.setLogId(1L);
        testTimeLogResponse.setTaskId(1L);
        testTimeLogResponse.setStartTime(LocalDateTime.now());
        testTimeLogResponse.setEndTime(LocalDateTime.now().plusHours(8));
        testTimeLogResponse.setHoursWorked(8.0);

        testTimeLogCreate = new CreateTimeLogDTO();
        testTimeLogCreate.setTaskId(1L);
        testTimeLogCreate.setProjectId(1L);
        testTimeLogCreate.setStartTime(LocalDateTime.now());
        testTimeLogCreate.setEndTime(LocalDateTime.now().plusHours(8));

        testTimeLogUpdate = new UpdateTimeLogDTO();
        testTimeLogUpdate.setDescription("Updated time log description");
    }

    // ========== POST /api/v1/timelogs ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateTimeLog_Success() throws Exception {
        // Arrange
        when(timeLogService.createTimeLog(any(CreateTimeLogDTO.class)))
                .thenReturn(testTimeLogResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/timelogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTimeLogCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Time log created successfully"))
                .andExpect(jsonPath("$.data.logId").value(1))
                .andExpect(jsonPath("$.data.hoursWorked").value(8.0));

        verify(timeLogService, times(1)).createTimeLog(any(CreateTimeLogDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateTimeLog_InvalidData() throws Exception {
        // Arrange
        when(timeLogService.createTimeLog(any(CreateTimeLogDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid time log data"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/timelogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTimeLogCreate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTimeLog_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/timelogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTimeLogCreate)))
                .andExpect(status().isUnauthorized());

        verify(timeLogService, never()).createTimeLog(any());
    }

    // ========== GET /api/v1/timelogs ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAllTimeLogs_Success() throws Exception {
        // Arrange
        List<TimeLogResponseDTO> timeLogs = Arrays.asList(testTimeLogResponse);
        when(timeLogService.getAllTimeLogs()).thenReturn(timeLogs);

        // Act & Assert
        mockMvc.perform(get("/api/v1/timelogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Time logs retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].logId").value(1));

        verify(timeLogService, times(1)).getAllTimeLogs();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllTimeLogs_EmptyList() throws Exception {
        // Arrange
        when(timeLogService.getAllTimeLogs()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/timelogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== GET /api/v1/timelogs/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTimeLogById_Success() throws Exception {
        // Arrange
        when(timeLogService.getTimeLogById(1L)).thenReturn(testTimeLogResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/timelogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Time log retrieved successfully"))
                .andExpect(jsonPath("$.data.logId").value(1));

        verify(timeLogService, times(1)).getTimeLogById(1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTimeLogById_NotFound() throws Exception {
        // Arrange
        when(timeLogService.getTimeLogById(999L))
                .thenThrow(new ResourceNotFoundException("Time log not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/timelogs/999"))
                .andExpect(status().isNotFound());
    }

    // ========== PATCH /api/v1/timelogs/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateTimeLog_Success() throws Exception {
        // Arrange
        TimeLogResponseDTO updatedResponse = new TimeLogResponseDTO();
        updatedResponse.setLogId(1L);
        updatedResponse.setDescription("Updated time log description");

        when(timeLogService.updateTimeLog(anyLong(), any(UpdateTimeLogDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/timelogs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTimeLogUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Time log updated successfully"))
                .andExpect(jsonPath("$.data.description").value("Updated time log description"));

        verify(timeLogService, times(1)).updateTimeLog(anyLong(), any(UpdateTimeLogDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateTimeLog_NotFound() throws Exception {
        // Arrange
        when(timeLogService.updateTimeLog(anyLong(), any(UpdateTimeLogDTO.class)))
                .thenThrow(new ResourceNotFoundException("Time log not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/timelogs/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTimeLogUpdate)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /api/v1/timelogs/{id} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteTimeLog_Success() throws Exception {
        // Arrange
        doNothing().when(timeLogService).deleteTimeLog(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/timelogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Time log deleted successfully"));

        verify(timeLogService, times(1)).deleteTimeLog(1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDeleteTimeLog_NotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Time log not found"))
                .when(timeLogService).deleteTimeLog(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/timelogs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTimeLog_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/timelogs/1"))
                .andExpect(status().isUnauthorized());

        verify(timeLogService, never()).deleteTimeLog(anyLong());
    }
}
