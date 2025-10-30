package com.ead.gearup.dto.project;

import com.ead.gearup.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProjectDTO {

    private String name;
    private String description;
    private LocalDate endDate;
    private ProjectStatus status;
}
