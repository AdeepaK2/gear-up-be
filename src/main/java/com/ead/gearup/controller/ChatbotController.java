package com.ead.gearup.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.service.auth.CurrentUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chatbot Integration", description = "API endpoints for chatbot integration")
@Slf4j
public class ChatbotController {

    private final AppointmentService appointmentService;
    private final CustomerService customerService;
    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;

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

        log.debug("Getting appointments for chatbot - customerId: {}, customerEmail: {}, status: {}, type: {}", 
                 customerId, customerEmail, status, type);

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
            try {
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
            } catch (Exception e) {
                // Log the error but return empty list instead of failing
                log.error("Error getting appointments for current customer: {}", e.getMessage(), e);
                appointments = List.of();
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

        log.debug("Getting appointment details for chatbot - appointmentId: {}", appointmentId);
        
        try {
            AppointmentResponseDTO appointment = appointmentService.getAppointmentById(appointmentId);

            ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                    .status("success")
                    .message("Appointment details retrieved successfully")
                    .data(appointment)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            log.debug("Successfully retrieved appointment details for appointmentId: {}", appointmentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting appointment details for chatbot - appointmentId: {}, error: {}", 
                     appointmentId, e.getMessage(), e);

            ApiResponseDTO<AppointmentResponseDTO> errorResponse = ApiResponseDTO.<AppointmentResponseDTO>builder()
                    .status("error")
                    .message("Failed to retrieve appointment details: " + e.getMessage())
                    .data(null)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            // Return appropriate HTTP status based on exception type
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized") || e.getMessage().contains("only view your own")) {
                return ResponseEntity.status(403).body(errorResponse);
            } else {
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get appointments for current authenticated customer (direct endpoint for chatbot)
     */
    @GetMapping("/appointments/current-customer")
    @Operation(
        summary = "Get appointments for current customer",
        description = "Retrieves appointments for the currently authenticated customer"
    )
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getCurrentCustomerAppointments(
            @RequestParam(required = false) String type,
            HttpServletRequest request) {

        log.debug("Getting appointments for current customer - type: {}", type);

        List<AppointmentResponseDTO> appointments;
        
        try {
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
            
            log.debug("Found {} appointments for current customer", appointments.size());
        } catch (Exception e) {
            log.error("Error getting appointments for current customer: {}", e.getMessage(), e);
            appointments = List.of();
        }

        String message = String.format("Found %d appointment(s) for current customer", appointments.size());
        
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
     * Debug endpoint to help diagnose appointment issues
     */
    @GetMapping("/debug/current-user")
    @Operation(
        summary = "Debug current user information",
        description = "Returns debug information about the current authenticated user"
    )
    public ResponseEntity<ApiResponseDTO<Object>> debugCurrentUser(HttpServletRequest request) {
        try {
            Long userId = currentUserService.getCurrentUserId();
            Long entityId = currentUserService.getCurrentEntityId();
            UserRole userRole = currentUserService.getCurrentUserRole();
            User currentUser = currentUserService.getCurrentUser();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", userId);
            debugInfo.put("entityId", entityId);
            debugInfo.put("userRole", userRole);
            debugInfo.put("userEmail", currentUser.getEmail());
            debugInfo.put("userName", currentUser.getName());
            
            // Try to find customer record
            if (userRole == UserRole.CUSTOMER) {
                Customer customer = customerRepository.findByUser(currentUser).orElse(null);
                if (customer != null) {
                    debugInfo.put("customerId", customer.getCustomerId());
                    debugInfo.put("customerFound", true);
                } else {
                    debugInfo.put("customerFound", false);
                    debugInfo.put("error", "Customer entity not found for user");
                }
            }
            
            ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                    .status("success")
                    .message("Debug information retrieved successfully")
                    .data(debugInfo)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting debug information: {}", e.getMessage(), e);
            
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("errorType", e.getClass().getSimpleName());
            
            ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                    .status("error")
                    .message("Failed to get debug information")
                    .data(errorInfo)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.ok(response);
        }
    }
}