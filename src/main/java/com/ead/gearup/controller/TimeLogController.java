package com.ead.gearup.controller;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.timelog.CreateTimeLogDTO;
import com.ead.gearup.dto.timelog.ProjectTimeLogSummaryDTO;
import com.ead.gearup.dto.timelog.TimeLogResponseDTO;
import com.ead.gearup.dto.timelog.UpdateTimeLogDTO;
import com.ead.gearup.service.TimeLogService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timelogs")
@SecurityRequirement(name = "bearerAuth")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(TimeLogService timeLogService) {
        this.timeLogService = timeLogService;
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @PostMapping
    public ResponseEntity<ApiResponseDTO<TimeLogResponseDTO>> createTimeLog(
            @Valid @RequestBody CreateTimeLogDTO dto,
            HttpServletRequest request) {

        TimeLogResponseDTO createdTimeLog = timeLogService.createTimeLog(dto);

        ApiResponseDTO<TimeLogResponseDTO> response = ApiResponseDTO.<TimeLogResponseDTO>builder()
                .status("success")
                .message("Time log created successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(createdTimeLog)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<TimeLogResponseDTO>>> getAllTimeLogs(HttpServletRequest request) {

        List<TimeLogResponseDTO> timeLogs = timeLogService.getAllTimeLogs();

        ApiResponseDTO<List<TimeLogResponseDTO>> response = ApiResponseDTO.<List<TimeLogResponseDTO>>builder()
                .status("success")
                .message("Time logs retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(timeLogs)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<TimeLogResponseDTO>> getTimeLogById(
            @PathVariable Long id,
            HttpServletRequest request) {

        TimeLogResponseDTO timeLog = timeLogService.getTimeLogById(id);

        ApiResponseDTO<TimeLogResponseDTO> response = ApiResponseDTO.<TimeLogResponseDTO>builder()
                .status("success")
                .message("Time log retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(timeLog)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<TimeLogResponseDTO>> updateTimeLog(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimeLogDTO dto,
            HttpServletRequest request) {

        TimeLogResponseDTO updatedTimeLog = timeLogService.updateTimeLog(id, dto);

        ApiResponseDTO<TimeLogResponseDTO> response = ApiResponseDTO.<TimeLogResponseDTO>builder()
                .status("success")
                .message("Time log updated successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(updatedTimeLog)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteTimeLog(
            @PathVariable Long id,
            HttpServletRequest request) {

        timeLogService.deleteTimeLog(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Time log deleted successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @GetMapping("/project/{projectId}/summary")
    public ResponseEntity<ApiResponseDTO<ProjectTimeLogSummaryDTO>> getProjectTimeLogSummary(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        ProjectTimeLogSummaryDTO summary = timeLogService.getProjectTimeLogSummary(projectId);

        ApiResponseDTO<ProjectTimeLogSummaryDTO> response = ApiResponseDTO.<ProjectTimeLogSummaryDTO>builder()
                .status("success")
                .message("Project time log summary retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(summary)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE})
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponseDTO<List<TimeLogResponseDTO>>> getTimeLogsByAppointment(
            @PathVariable Long appointmentId,
            HttpServletRequest request) {

        List<TimeLogResponseDTO> timeLogs = timeLogService.getTimeLogsByAppointment(appointmentId);

        ApiResponseDTO<List<TimeLogResponseDTO>> response = ApiResponseDTO.<List<TimeLogResponseDTO>>builder()
                .status("success")
                .message("Appointment time logs retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(timeLogs)
                .build();

        return ResponseEntity.ok(response);
    }
}
