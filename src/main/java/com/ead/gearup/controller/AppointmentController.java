package com.ead.gearup.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.employee.EmployeeAvailableSlotsDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.auth.CurrentUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Appointment Management", description = "Operations for managing service appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;

    // @RequiresRole({ UserRole.CUSTOMER })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a new appointment",
        description = "Creates a new service appointment for a customer's vehicle"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Appointment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid appointment data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> createAppointment(
            @RequestBody @Valid 
            @Parameter(description = "Appointment details", required = true)
            AppointmentCreateDTO appointmentCreateDTO, HttpServletRequest request) {
        AppointmentResponseDTO appointmentResponseDTO = appointmentService.createAppointment(appointmentCreateDTO);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment created successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    

    @PatchMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> updateAppointment(@PathVariable Long id,
            @RequestBody @Valid AppointmentUpdateDTO appointmentUpdateDTO, HttpServletRequest request) {

        AppointmentResponseDTO appointmentResponseDTO = appointmentService.updateAppointment(id, appointmentUpdateDTO);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment updated successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id,
    HttpServletRequest request) {
        AppointmentResponseDTO appointmentResponseDTO = appointmentService.getAppointmentById(id);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment updated successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAllAppointments(HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments();

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAppointment(@PathVariable Long id, HttpServletRequest request) {
        appointmentService.deleteAppointment(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Appointment canceled successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getUpcomingAppointments (HttpServletRequest request) {
        Long employeeId = currentUserService.getCurrentEntityId();
        List<AppointmentResponseDTO> appointments = appointmentService.getUpcomingAppointmentsForEmployee(employeeId);

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Upcoming appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    // Dashboard - employee's appointments list
    @GetMapping("/employee")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsForEmployee (HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsForEmployee();

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Employee appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vehicles")
    @Operation(summary = "Get vehicles for the logged-in customer to book appointments")
    public ResponseEntity<ApiResponseDTO<List<VehicleResponseDTO>>> getVehiclesForAppointments(
            HttpServletRequest request) {

        List<VehicleResponseDTO> vehicles = appointmentService.getVehiclesForCurrentCustomer();

        ApiResponseDTO<List<VehicleResponseDTO>> response = ApiResponseDTO.<List<VehicleResponseDTO>>builder()
                .status("success")
                .message("Vehicles retrieved successfully for appointment booking")
                .data(vehicles)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter-by-date")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsByDate(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, HttpServletRequest request) {
            Long employeeId = currentUserService.getCurrentEntityId();

            List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByDate(employeeId, date);

            ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                    .status("success")
                    .message("Appointments for the date retrieved successfully")
                    .data(appointments)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);
    }

    @GetMapping("/by-month")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsByMonthAndStatuses(
        @RequestParam int year,
        @RequestParam int month,
        @RequestParam(required = false) List<AppointmentStatus> statuses,
        HttpServletRequest request) {

            if (statuses == null || statuses.isEmpty()) {
                statuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED);
            }

            List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByMonthANDStatuses(year, month, statuses);

            ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                    .status("success")
                    .message("Appointments for the month retrieved successfully")
                    .data(appointments)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.ok(response);
        }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> searchAppointments(
        @RequestParam("keyword") String keyword,
        HttpServletRequest request) {

            List<AppointmentResponseDTO> results = appointmentService.searchAppointments(keyword);

            ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                    .status("success")
                    .message("Search results retrieved successfully")
                    .data(results)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.ok(response);
        }

    // Employee's upcoming appointments
    @GetMapping("/employee/upcoming")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getUpcomingAppointmentsForEmployee(HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getUpcomingAppointmentsForEmployee();
        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Upcoming appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/available-slots")
    public ResponseEntity<ApiResponseDTO<List<EmployeeAvailableSlotsDTO>>> getEmployeeAvailableSlots(
        @RequestParam("date") LocalDate date,
        HttpServletRequest request) {
            List<EmployeeAvailableSlotsDTO> slots = appointmentService.getAvailableSlotsForEmployee(date);
            ApiResponseDTO<List<EmployeeAvailableSlotsDTO>> response = ApiResponseDTO.<List<EmployeeAvailableSlotsDTO>>builder()
                    .status("success")
                    .message("Available slots retrieved successfully")
                    .data(slots)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.ok(response);    

    }

}
