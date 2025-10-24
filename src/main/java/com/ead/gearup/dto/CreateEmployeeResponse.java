package com.ead.gearup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeResponse {
    private EmployeeDTO employee;
    private String temporaryPassword;
    private String message;
}
