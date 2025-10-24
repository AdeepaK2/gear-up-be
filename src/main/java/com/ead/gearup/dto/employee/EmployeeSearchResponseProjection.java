package com.ead.gearup.dto.employee;

import java.time.LocalDate;

public interface EmployeeSearchResponseProjection {
    Long getEmployeeId();

    String getName();

    String getEmail();

    String getSpecialization();

    LocalDate getHireDate();
}
