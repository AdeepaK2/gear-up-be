package com.ead.gearup.dto.project;

import com.ead.gearup.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private ProjectStatus status;
}
