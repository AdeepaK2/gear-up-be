package com.ead.gearup.dto.employee;

import java.time.LocalDate;

import com.ead.gearup.enums.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProjectDetailResponseDTO {
    private String customerName;
    private String vehicleModel;
    private LocalDate endDate;
    private LocalDate startDate;
    private ProjectStatus status;
    private Long completionDays;
    
}
