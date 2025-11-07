package com.ead.gearup.dto.task;

import com.ead.gearup.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusUpdateDTO {
    @NotNull
    private TaskStatus status;
}
