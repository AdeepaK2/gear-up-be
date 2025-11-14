package com.ead.gearup.dto.timelog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectTimeLogSummaryDTO {
    private Long projectId;
    private String projectName;
    private Integer totalEstimatedHours;
    private Double totalLoggedHours;
    private Double remainingHours;
    private Double percentageUsed;
    private Boolean isOverBudget;
}
