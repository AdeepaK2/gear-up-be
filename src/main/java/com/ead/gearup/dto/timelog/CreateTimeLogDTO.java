package com.ead.gearup.dto.timelog;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTimeLogDTO {
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long taskId;
    private Long projectId;
}
