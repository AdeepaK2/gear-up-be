package com.ead.gearup.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmployeeDTO {

    private String specialization;
    private LocalDate hireDate;
    private String profileImage;
    private String phoneNumber;
}
