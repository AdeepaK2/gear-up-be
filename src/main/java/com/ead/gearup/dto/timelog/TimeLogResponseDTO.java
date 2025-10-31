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
}
