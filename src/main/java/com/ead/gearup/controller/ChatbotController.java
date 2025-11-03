package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chatbot Integration", description = "API endpoints for chatbot integration")
public class ChatbotController {

    private final AppointmentService appointmentService;
    private final CustomerService customerService;

    /**
     * Get appointments for chatbot - can query by customer ID or email
     */
    @GetMapping("/appointments")
    @Operation(
        summary = "Get appointments for chatbot",
        description = "Retrieves appointments for chatbot integration. Can filter by status and date."
    )
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAppointmentsForChatbot(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            HttpServletRequest request) {

        List<AppointmentResponseDTO> appointments;

        if (customerId != null) {
            // Get appointments by customer ID
            if ("available".equals(type)) {
                appointments = appointmentService.getAvailableAppointmentsByCustomerId(customerId);
            } else if ("upcoming".equals(type)) {
                appointments = appointmentService.getUpcomingAppointmentsByCustomerId(customerId);
            } else {
                appointments = appointmentService.getAppointmentsByCustomerId(customerId);
            }
        } else if (customerEmail != null) {
            // Get customer by email first, then get appointments
            Long customerIdFromEmail = customerService.getCustomerIdByEmail(customerEmail);
            if ("available".equals(type)) {
                appointments = appointmentService.getAvailableAppointmentsByCustomerId(customerIdFromEmail);
            } else if ("upcoming".equals(type)) {
                appointments = appointmentService.getUpcomingAppointmentsByCustomerId(customerIdFromEmail);
            } else {
                appointments = appointmentService.getAppointmentsByCustomerId(customerIdFromEmail);
            }
        } else {
            // If no specific customer, get current customer's appointments
            if ("available".equals(type)) {
                appointments = appointmentService.getAllAppointmentsForCurrentCustomer()
                    .stream()
                    .filter(apt -> "PENDING".equals(apt.getStatus()))
                    .toList();
            } else if ("upcoming".equals(type)) {
                appointments = appointmentService.getAllAppointmentsForCurrentCustomer()
                    .stream()
                    .filter(apt -> apt.getAppointmentDate().isAfter(java.time.LocalDate.now()) || 
                                 apt.getAppointmentDate().equals(java.time.LocalDate.now()))
                    .toList();
            } else {
                appointments = appointmentService.getAllAppointmentsForCurrentCustomer();
            }
        }

        String message = String.format("Found %d appointment(s)", appointments.size());
        
        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message(message)
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific appointment details for chatbot
     */
    @GetMapping("/appointments/{appointmentId}")
    @Operation(
        summary = "Get specific appointment for chatbot",
        description = "Retrieves detailed information about a specific appointment"
    )
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> getAppointmentForChatbot(
            @PathVariable Long appointmentId,
            HttpServletRequest request) {

        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(appointmentId);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment details retrieved successfully")
                .data(appointment)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }
}