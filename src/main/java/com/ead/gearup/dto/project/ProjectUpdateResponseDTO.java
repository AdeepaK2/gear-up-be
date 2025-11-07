package com.ead.gearup.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateResponseDTO {
    
    private Long id;
    
    private Long projectId;
    
    private String projectName;
    
    private Long employeeId;
    
    private String employeeName;
    
    private String message;
    
    private Integer completedTasks;
    
    private Integer totalTasks;
    
    private Double additionalCost;
    
    private String additionalCostReason;
    
    private String estimatedCompletionDate;
    
    private String updateType;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
