package com.ead.gearup.dto.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignEmployeesDTO {

    @NotEmpty(message = "At least one employee must be assigned")
    private List<Long> employeeIds;
    
    private Long mainRepresentativeEmployeeId;
}
