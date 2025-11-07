package com.ead.gearup.dto.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectReportDTO {
    @NotEmpty(message = "At least one task must be selected")
    private List<Long> taskIds;
    
    private String notes;
    
    private List<@Valid ExtraChargeDTO> extraCharges;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExtraChargeDTO {
        private String description;
        private Double amount;
    }
}

