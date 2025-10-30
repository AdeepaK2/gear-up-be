package com.ead.gearup.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeProjectResponseDTO {
    private Long projectId;
    private String projectName;
    
}
