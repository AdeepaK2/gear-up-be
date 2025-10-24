package com.ead.gearup.dto.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetailsResponseDTO {

    private Long id;

    private Long appointmentId;
    private Long customerId;

    private Long vehicleId;
    private String vehicleName;
    private String vehicleDetails;

    private String consultationType;
    private LocalDate consultationDate;

    private Long employeeId;
    private String employeeName;

    private ProjectStatus status;

    private List<TaskResponseDTO> services;

    private List<String> additionalRequests;

    private Double totalEstimatedCost;
    private Double totalAcceptedCost;
    private Integer acceptedServicesCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String additionalRequest;
    private String referenceFilePath;
}
