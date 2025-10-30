package com.ead.gearup.dto.task;

import com.ead.gearup.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateDTO {
    private String name;
    private String description;
    private Integer estimatedHours;
    private Double estimatedCost;
    private TaskStatus status;
    private String category;
    private String priority;
    private String notes;
}

