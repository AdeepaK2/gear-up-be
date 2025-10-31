package com.ead.gearup.dto.employee;

import java.time.LocalDate;

import com.ead.gearup.dto.response.UserResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSearchResponseDTO {
    private Long employeeId;
    private UserResponseDTO user;
    private String specialization;
    private LocalDate hireDate;
}
