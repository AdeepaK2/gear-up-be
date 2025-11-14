package com.ead.gearup.dto.timelog;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeLogResponseDTO {
    private Long logId;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double hoursWorked;
    private LocalDateTime loggedAt;
    private Long taskId;
    
    // Employee details
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    
    // Project details (optional)
    private Long projectId;
    private String projectName;
    
    // Appointment details (optional)
    private Long appointmentId;
    private String appointmentDate;
}
