package com.ead.gearup.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateDTO {
    private String name;
    private String description;
    private Integer estimatedHours;
    private Double estimatedCost;
    private Long appointmentId;
    private Long employeeId;  // Optional - can be assigned later
    private String category;
    private String priority;
    private String notes;
    private String requestedBy;
}

