package com.ead.gearup.dto.employee;

import com.ead.gearup.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeProjectResponseDTO {
    private Long projectId;
    private String projectName;
    private String description;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate dueDate;
    private Long customerId;
    private String customerName;
    private Long vehicleId;
    private String vehicleName;
    private Long appointmentId;
    private List<AssignedEmployeeDTO> assignedEmployees;
    private Long mainRepresentativeEmployeeId;
    private String mainRepresentativeEmployeeName;
    private Boolean isMainRepresentative;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssignedEmployeeDTO {
        private Long employeeId;
        private String name;
        private String specialization;
    }
}
