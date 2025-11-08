package com.ead.gearup.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.employee.EmployeeAvailableSlotsDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.VehicleService;
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
    private final VehicleService vehicleService;
    private final CurrentUserService currentUserService;
    private final com.ead.gearup.service.ShopSettingsService shopSettingsService;

    // @RequiresRole({ UserRole.CUSTOMER })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new appointment", description = "Creates a new service appointment for a customer's vehicle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Appointment created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid appointment data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> createAppointment(
            @RequestBody @Valid @Parameter(description = "Appointment details", required = true) AppointmentCreateDTO appointmentCreateDTO,
            HttpServletRequest request) {
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

    @GetMapping
    @Operation(summary = "Get all appointments", description = "Retrieves all appointments")
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

    @GetMapping("/customer")
    @Operation(summary = "Get all appointments for current customer", description = "Retrieves all appointments for the authenticated customer")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAllAppointmentsForCurrentCustomer(HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointmentsForCurrentCustomer();

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID", description = "Retrieves a specific appointment by its ID")
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id,
            HttpServletRequest request) {
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(id);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment retrieved successfully")
                .data(appointment)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{id}")
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete appointment", description = "Deletes an appointment by its ID")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAppointment(@PathVariable Long id, HttpServletRequest request) {
        appointmentService.deleteAppointment(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Appointment deleted successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get appointments by customer ID (for chatbot integration)
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get appointments by customer ID", description = "Retrieves all appointments for a specific customer (for chatbot/admin use)")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsByCustomerId(
            @PathVariable Long customerId, HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByCustomerId(customerId);

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Customer appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get available appointments by customer ID (PENDING status only)
     */
    @GetMapping("/customer/{customerId}/available")
    @Operation(summary = "Get available appointments by customer ID", description = "Retrieves available (PENDING) appointments for a specific customer")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAvailableAppointmentsByCustomerId(
            @PathVariable Long customerId, HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAvailableAppointmentsByCustomerId(customerId);

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Available appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get upcoming appointments by customer ID (future dates only)
     */
    @GetMapping("/customer/{customerId}/upcoming")
    @Operation(summary = "Get upcoming appointments by customer ID", description = "Retrieves upcoming appointments for a specific customer")
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getUpcomingAppointmentsByCustomerId(
            @PathVariable Long customerId, HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getUpcomingAppointmentsByCustomerId(customerId);

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
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsForEmployee(
            HttpServletRequest request) {
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

        List<VehicleResponseDTO> vehicles = vehicleService.getVehiclesForCurrentCustomer();

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
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
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

        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByMonthANDStatuses(year, month,
                statuses);

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
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getUpcomingAppointmentsForEmployee(
            HttpServletRequest request) {
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
        ApiResponseDTO<List<EmployeeAvailableSlotsDTO>> response = ApiResponseDTO
                .<List<EmployeeAvailableSlotsDTO>>builder()
                .status("success")
                .message("Available slots retrieved successfully")
                .data(slots)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shop-settings")
    @Operation(
        summary = "Get shop settings (Public)",
        description = "Retrieve shop operating hours and days for appointment scheduling. This is a public endpoint accessible without authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Shop settings retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO>> getShopSettingsPublic(
            HttpServletRequest request) {
        
        com.ead.gearup.dto.settings.ShopSettingsDTO settings = shopSettingsService.getShopSettings();

        ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO> response = 
            ApiResponseDTO.<com.ead.gearup.dto.settings.ShopSettingsDTO>builder()
                .status("success")
                .message("Shop settings retrieved successfully")
                .data(settings)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }
}

