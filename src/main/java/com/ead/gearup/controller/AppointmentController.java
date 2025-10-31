package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.service.AppointmentService;

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

    @GetMapping
    @Operation(
        summary = "Get all appointments for current customer",
        description = "Retrieves all appointments for the authenticated customer"
    )
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAllAppointments(HttpServletRequest request) {
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
    @Operation(
        summary = "Get appointment by ID",
        description = "Retrieves a specific appointment by its ID"
    )
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id, HttpServletRequest request) {
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
    @Operation(
        summary = "Delete appointment",
        description = "Deletes an appointment by its ID"
    )
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
}
