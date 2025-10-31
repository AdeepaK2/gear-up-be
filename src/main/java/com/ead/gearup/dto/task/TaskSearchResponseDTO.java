package com.ead.gearup.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchResponseDTO {
    private Long taskId;
    private String name;
    private String description;
    private Integer estimatedHours;
    private Double cost;
    private String status;
    private boolean isAssignedProject;
}
