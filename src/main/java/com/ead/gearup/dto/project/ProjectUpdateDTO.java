package com.ead.gearup.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateDTO {
    
    @NotBlank(message = "Update message is required")
    @Size(min = 10, max = 2000, message = "Update message must be between 10 and 2000 characters")
    private String message;
    
    private Integer completedTasks;
    
    private Integer totalTasks;
    
    private Double additionalCost;
    
    private String additionalCostReason;
    
    private String estimatedCompletionDate;
    
    private String updateType; // PROGRESS, COST_CHANGE, DELAY, COMPLETION, GENERAL
    
    private List<TaskCompletionDTO> taskCompletions;
    
    private Integer overallCompletionPercentage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCompletionDTO {
        private Long taskId;
        private String taskName;
        private Boolean isCompleted;
        private Integer completionPercentage;
    }
}
