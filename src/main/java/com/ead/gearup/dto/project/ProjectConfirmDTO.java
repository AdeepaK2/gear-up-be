package com.ead.gearup.dto.project;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectConfirmDTO {
    @NotNull(message = "Total accepted cost is required")
    private Double totalAcceptedCost;

    @NotNull(message = "Accepted services count is required")
    private Integer acceptedServicesCount;
}
