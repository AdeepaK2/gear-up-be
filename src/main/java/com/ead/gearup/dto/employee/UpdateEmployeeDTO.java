package com.ead.gearup.dto.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeDTO {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    // @Email(message = "Email should be valid")
    // private String email;

    // @Size(min = 8, message = "Password must be at least 8 characters long")
    // private String password;

    // private String role;

    private String specialization;

    private LocalDate hireDate;

    private String profileImage;

    private String phoneNumber;
}
