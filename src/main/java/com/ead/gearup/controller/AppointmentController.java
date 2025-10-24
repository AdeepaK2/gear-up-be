package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

import com.ead.gearup.dto.vehicle.VehicleResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
