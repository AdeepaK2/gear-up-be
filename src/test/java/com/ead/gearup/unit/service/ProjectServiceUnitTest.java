package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.project.CreateProjectDTO;
import com.ead.gearup.dto.project.ProjectResponseDTO;
import com.ead.gearup.dto.project.UpdateProjectDTO;
import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.*;
import com.ead.gearup.model.*;
import com.ead.gearup.repository.*;
import com.ead.gearup.service.ProjectService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.ProjectDTOConverter;
import com.ead.gearup.util.TaskDTOConverter;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectDTOConverter projectDTOConverter;

    @Mock
    private TaskDTOConverter taskDTOConverter;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private Appointment testAppointment;
    private Vehicle testVehicle;
    private Customer testCustomer;
    private Task testTask;
    private CreateProjectDTO createDTO;
    private UpdateProjectDTO updateDTO;
    private ProjectResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);

        // Setup test appointment
        testAppointment = new Appointment();
        testAppointment.setAppointmentId(1L);

        // Setup test vehicle
        testVehicle = new Vehicle();
        testVehicle.setVehicleId(1L);

        // Setup test task
        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setName("Oil Change");

        // Setup test project
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setStatus(ProjectStatus.CREATED);
        testProject.setCustomer(testCustomer);
        testProject.setAppointment(testAppointment);
        testProject.setVehicle(testVehicle);
        testProject.setTasks(Arrays.asList(testTask));
        testProject.setAssignedEmployees(Arrays.asList());
        testProject.setStartDate(LocalDate.now());

        // Setup DTOs
        createDTO = new CreateProjectDTO();
        createDTO.setAppointmentId(1L);
        createDTO.setVehicleId(1L);
        createDTO.setTaskIds(Arrays.asList(1L));

        updateDTO = new UpdateProjectDTO();
        updateDTO.setName("Updated Project");

        responseDTO = new ProjectResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("Test Project");
    }

    // ========== createProject() Tests ==========
    @Test
    void testCreateProject_Success() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(taskRepository.findAllById(any())).thenReturn(Arrays.asList(testTask));
        when(projectDTOConverter.convertToEntity(any())).thenReturn(testProject);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        ProjectResponseDTO result = projectService.createProject(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertEquals(ProjectStatus.CREATED, testProject.getStatus());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testCreateProject_AppointmentNotFound() {
        // Arrange
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());
        createDTO.setAppointmentId(999L);

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class, () -> projectService.createProject(createDTO));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testCreateProject_VehicleNotFound() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());
        createDTO.setVehicleId(999L);

        // Act & Assert
        assertThrows(VehicleNotFoundException.class, () -> projectService.createProject(createDTO));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testCreateProject_NoTasksFound() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(taskRepository.findAllById(any())).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> projectService.createProject(createDTO));
        verify(projectRepository, never()).save(any());
    }

    // ========== updateProject() Tests ==========
    @Test
    void testUpdateProject_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(projectDTOConverter).updateEntityFromDto(any(), any());
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        ProjectResponseDTO result = projectService.updateProject(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(projectRepository, times(1)).save(testProject);
    }

    @Test
    void testUpdateProject_NotFound() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> projectService.updateProject(999L, updateDTO));
        verify(projectRepository, never()).save(any());
    }

    // ========== getProjectById() Tests ==========
    @Test
    void testGetProjectById_Success_AsAdmin() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        ProjectResponseDTO result = projectService.getProjectById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetProjectById_Success_AsCustomer() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        ProjectResponseDTO result = projectService.getProjectById(1L);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetProjectById_UnauthorizedCustomer() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(999L); // Different customer

        // Act & Assert
        assertThrows(UnauthorizedProjectAccessException.class, 
            () -> projectService.getProjectById(1L));
    }

    @Test
    void testGetProjectById_NotFound() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> projectService.getProjectById(999L));
    }

    // ========== getAllProjects() Tests ==========
    @Test
    void testGetAllProjects_AsAdmin() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        when(projectRepository.findAll()).thenReturn(projects);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        List<ProjectResponseDTO> result = projectService.getAllProjects();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllProjects_AsCustomer() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(currentUserService.getCurrentUserRole()).thenReturn(UserRole.CUSTOMER);
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(projectRepository.findAll()).thenReturn(projects);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        List<ProjectResponseDTO> result = projectService.getAllProjects();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== deleteProject() Tests ==========
    @Test
    void testDeleteProject_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(projectRepository).delete(testProject);

        // Act
        projectService.deleteProject(1L);

        // Assert
        verify(projectRepository, times(1)).delete(testProject);
    }

    @Test
    void testDeleteProject_NotFound() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> projectService.deleteProject(999L));
        verify(projectRepository, never()).delete(any());
    }

    // ========== updateProjectStatus() Tests ==========
    @Test
    void testUpdateProjectStatus_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectDTOConverter.convertToResponseDto(any())).thenReturn(responseDTO);

        // Act
        ProjectResponseDTO result = projectService.updateProjectStatus(1L, ProjectStatus.IN_PROGRESS);

        // Assert
        assertNotNull(result);
        assertEquals(ProjectStatus.IN_PROGRESS, testProject.getStatus());
        verify(projectRepository, times(1)).save(testProject);
    }

    @Test
    void testUpdateProjectStatus_InvalidTransition() {
        // Arrange
        testProject.setStatus(ProjectStatus.COMPLETED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> projectService.updateProjectStatus(1L, ProjectStatus.CANCELLED));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testUpdateProjectStatus_NotFound() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, 
            () -> projectService.updateProjectStatus(999L, ProjectStatus.IN_PROGRESS));
    }
}
