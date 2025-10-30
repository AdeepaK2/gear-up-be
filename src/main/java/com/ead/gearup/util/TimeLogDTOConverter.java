package com.ead.gearup.util;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.timelog.CreateTimeLogDTO;
import com.ead.gearup.dto.timelog.TimeLogResponseDTO;
import com.ead.gearup.dto.timelog.UpdateTimeLogDTO;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.Task;
import com.ead.gearup.model.TimeLog;
import com.ead.gearup.model.Project;

@Component
public class TimeLogDTOConverter {

    // Convert Create DTO -> Entity 
    public TimeLog convertToEntity(CreateTimeLogDTO createTimeLogDTO, Employee employee, Task task, Project project) {
        TimeLog timeLog = new TimeLog();
        timeLog.setEmployee(employee);
        timeLog.setTask(task);
        timeLog.setProject(project);
        timeLog.setDescription(createTimeLogDTO.getDescription());
        timeLog.setStartTime(createTimeLogDTO.getStartTime());
        timeLog.setEndTime(createTimeLogDTO.getEndTime());
        return timeLog;
    }

    // Convert Entity -> Response DTO
    public TimeLogResponseDTO convertToResponseDTO(TimeLog timeLog) {
        TimeLogResponseDTO responseDTO = new TimeLogResponseDTO();
        responseDTO.setLogId(timeLog.getLogId());
        responseDTO.setDescription(timeLog.getDescription());
        responseDTO.setStartTime(timeLog.getStartTime());
        responseDTO.setEndTime(timeLog.getEndTime());
        responseDTO.setHoursWorked(timeLog.getHoursWorked() != null 
            ? timeLog.getHoursWorked() 
            : calculateHoursWorked(timeLog));
        responseDTO.setLoggedAt(timeLog.getLoggedAt());
        responseDTO.setTaskId(timeLog.getTask().getTaskId());
        return responseDTO;
    }

    // Update Entity from DTO
    public void updateEntityFromDTO(TimeLog timeLog, UpdateTimeLogDTO updateTimeLogDTO) {
        if (updateTimeLogDTO.getDescription() != null) {
            timeLog.setDescription(updateTimeLogDTO.getDescription());
        }
        if (updateTimeLogDTO.getStartTime() != null) {
            timeLog.setStartTime(updateTimeLogDTO.getStartTime());
        }
        if (updateTimeLogDTO.getEndTime() != null) {
            timeLog.setEndTime(updateTimeLogDTO.getEndTime());
        }
    }

    private Double calculateHoursWorked(TimeLog timeLog) {
        if (timeLog.getStartTime() != null && timeLog.getEndTime() != null) {
            return Duration.between(timeLog.getStartTime(), timeLog.getEndTime()).getSeconds() / 3600.0;
        }
        return 0.0;
    }
}
