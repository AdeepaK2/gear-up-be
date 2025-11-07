package com.ead.gearup.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ead.gearup.dto.employee.EmployeeProjectDetailResponseDTO;
import com.ead.gearup.dto.employee.EmployeeProjectResponseDTO;
import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.ProjectConfirmDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.project.ProjectStatusUpdateDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.dto.task.TaskStatusUpdateDTO;
import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.exception.ProjectNotFoundException;
import com.ead.gearup.service.ProjectService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectResponseDTO testProjectResponse;
    private CreateProjectDTO testProjectCreate;
    private UpdateProjectDTO testProjectUpdate;

    @BeforeEach
    void setUp() {
        testProjectResponse = new ProjectResponseDTO();
        testProjectResponse.setId(1L);
        testProjectResponse.setStatus(ProjectStatus.IN_PROGRESS);
        testProjectResponse.setCustomerId(1L);
        testProjectResponse.setName("Test Project");

        testProjectCreate = new CreateProjectDTO();
        testProjectCreate.setAppointmentId(1L);
        testProjectCreate.setVehicleId(1L);

        testProjectUpdate = new UpdateProjectDTO();
        testProjectUpdate.setDescription("Updated project description");
    }

    // ========== POST /api/v1/projects ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateProject_Success() throws Exception {
        // Arrange
        when(projectService.createProject(any(CreateProjectDTO.class)))
                .thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProjectCreate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Project created successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(projectService, times(1)).createProject(any(CreateProjectDTO.class));
    }

    // ========== PATCH /api/v1/projects/{id} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateProject_Success() throws Exception {
        // Arrange
        ProjectResponseDTO updatedResponse = new ProjectResponseDTO();
        updatedResponse.setId(1L);
        updatedResponse.setDescription("Updated project description");

        when(projectService.updateProject(anyLong(), any(UpdateProjectDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProjectUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.description").value("Updated project description"));

        verify(projectService, times(1)).updateProject(anyLong(), any(UpdateProjectDTO.class));
    }

    // ========== GET /api/v1/projects/{id} ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetProjectById_Success() throws Exception {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(projectService, times(1)).getProjectById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetProjectById_NotFound() throws Exception {
        // Arrange
        when(projectService.getProjectById(999L))
                .thenThrow(new ProjectNotFoundException("Project not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/projects ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAllProjects_Success() throws Exception {
        // Arrange
        List<ProjectResponseDTO> projects = Arrays.asList(testProjectResponse);
        when(projectService.getAllProjects()).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(projectService, times(1)).getAllProjects();
    }

    // ========== DELETE /api/v1/projects/{id} ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteProject_Success() throws Exception {
        // Arrange
        doNothing().when(projectService).deleteProject(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Project deleted successfully"));

        verify(projectService, times(1)).deleteProject(1L);
    }

    // ========== GET /api/v1/projects/status-count ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetProjectCountByStatus_Success() throws Exception {
        // Arrange
        Long employeeId = 1L;
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("IN_PROGRESS", 5L);
        statusCount.put("COMPLETED", 3L);

        when(currentUserService.getCurrentEntityId()).thenReturn(employeeId);
        when(projectService.getProjectCountByStatus(employeeId)).thenReturn(statusCount);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects/status-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(projectService, times(1)).getProjectCountByStatus(employeeId);
    }

    // ========== GET /api/v1/projects/my-assigned ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetMyAssignedProjects_Success() throws Exception {
        // Arrange
        EmployeeProjectResponseDTO employeeProject = new EmployeeProjectResponseDTO();
        employeeProject.setProjectId(1L);
        List<EmployeeProjectResponseDTO> projects = Arrays.asList(employeeProject);

        when(projectService.getAssignedProjectsForCurrentEmployee()).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects/my-assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Assigned projects retrieved successfully"));

        verify(projectService, times(1)).getAssignedProjectsForCurrentEmployee();
    }

    // ========== PATCH /api/v1/projects/{projectId}/services/{taskId}/status ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUpdateServiceStatus_Success() throws Exception {
        // Arrange
        TaskStatusUpdateDTO statusUpdate = new TaskStatusUpdateDTO();
        TaskResponseDTO taskResponse = new TaskResponseDTO();
        taskResponse.setTaskId(1L);

        when(projectService.updateServiceStatus(anyLong(), anyLong(), any(TaskStatusUpdateDTO.class)))
                .thenReturn(taskResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/projects/1/services/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Service status updated successfully"));

        verify(projectService, times(1)).updateServiceStatus(anyLong(), anyLong(), any(TaskStatusUpdateDTO.class));
    }

    // ========== POST /api/v1/projects/{projectId}/confirm ==========
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testConfirmServices_Success() throws Exception {
        // Arrange
        ProjectConfirmDTO confirmDTO = new ProjectConfirmDTO();
        confirmDTO.setTotalAcceptedCost(500.0);
        confirmDTO.setAcceptedServicesCount(2);

        when(projectService.confirmServices(anyLong(), any(ProjectConfirmDTO.class)))
                .thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/projects/1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Project confirmed successfully"));

        verify(projectService, times(1)).confirmServices(anyLong(), any(ProjectConfirmDTO.class));
    }

    // ========== GET /api/v1/projects/my-assigned/{projectId} ==========
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetMyAssignedProjectDetail_Success() throws Exception {
        // Arrange
        EmployeeProjectDetailResponseDTO projectDetail = new EmployeeProjectDetailResponseDTO();

        when(projectService.getAssignedProjectDetail(1L)).thenReturn(projectDetail);

        // Act & Assert
        mockMvc.perform(get("/api/v1/projects/my-assigned/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Assigned project detail retrieved successfully"));

        verify(projectService, times(1)).getAssignedProjectDetail(1L);
    }

    // ========== PATCH /api/v1/projects/{projectId}/status ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateProjectStatus_Success() throws Exception {
        // Arrange
        ProjectStatusUpdateDTO statusUpdate = new ProjectStatusUpdateDTO();
        statusUpdate.setStatus(ProjectStatus.CANCELLED);

        ProjectResponseDTO updatedProject = new ProjectResponseDTO();
        updatedProject.setId(1L);
        updatedProject.setStatus(ProjectStatus.CANCELLED);

        when(projectService.updateProjectStatus(anyLong(), any(ProjectStatus.class)))
                .thenReturn(updatedProject);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/projects/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(projectService, times(1)).updateProjectStatus(anyLong(), any(ProjectStatus.class));
    }
}
