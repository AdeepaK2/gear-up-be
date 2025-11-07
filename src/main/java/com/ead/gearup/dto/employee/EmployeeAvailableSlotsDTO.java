package com.ead.gearup.dto.employee;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAvailableSlotsDTO {
    private LocalDate date;
    private List<LocalTime> availableSlots;
}
