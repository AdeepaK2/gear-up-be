package com.ead.gearup.controller;

import com.ead.gearup.dto.CreateEmployeeRequest;
import com.ead.gearup.dto.CreateEmployeeResponse;
import com.ead.gearup.dto.EmployeeDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.EmployeeManagementService;
import com.ead.gearup.validation.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Admin - Employee Management", description = "Endpoints for managing employee accounts (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    @PostMapping
    @RequiresRole({ UserRole.ADMIN })
    @Operation(summary = "Create new employee account", description = "Creates a new employee account with auto-generated temporary password sent via email")
    public ResponseEntity<ApiResponseDTO<CreateEmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            HttpServletRequest httpRequest) {

        CreateEmployeeResponse response = employeeManagementService.createEmployee(request);

        ApiResponseDTO<CreateEmployeeResponse> apiResponse = ApiResponseDTO.<CreateEmployeeResponse>builder()
                .status("success")
                .message("Employee account created successfully. Temporary password sent to " + request.getEmail())
                .data(response)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping
    @RequiresRole({ UserRole.ADMIN })
    @Operation(summary = "Get all employees", description = "Retrieves list of all employee accounts")
    public ResponseEntity<ApiResponseDTO<List<EmployeeDTO>>> getAllEmployees(HttpServletRequest httpRequest) {
        List<EmployeeDTO> employees = employeeManagementService.getAllEmployees();

        ApiResponseDTO<List<EmployeeDTO>> apiResponse = ApiResponseDTO.<List<EmployeeDTO>>builder()
                .status("success")
                .message("Employees retrieved successfully")
                .data(employees)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{employeeId}/deactivate")
    @RequiresRole({ UserRole.ADMIN })
    @Operation(summary = "Deactivate employee account", description = "Deactivates an employee account")
    public ResponseEntity<ApiResponseDTO<Void>> deactivateEmployee(
            @PathVariable Long employeeId,
            HttpServletRequest httpRequest) {

        employeeManagementService.deactivateEmployee(employeeId);

        ApiResponseDTO<Void> apiResponse = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Employee account deactivated successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{employeeId}/reactivate")
    @RequiresRole({ UserRole.ADMIN })
    @Operation(summary = "Reactivate employee account", description = "Reactivates a deactivated employee account")
    public ResponseEntity<ApiResponseDTO<Void>> reactivateEmployee(
            @PathVariable Long employeeId,
            HttpServletRequest httpRequest) {

        employeeManagementService.reactivateEmployee(employeeId);

        ApiResponseDTO<Void> apiResponse = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Employee account reactivated successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{employeeId}/resend-password")
    @RequiresRole({ UserRole.ADMIN })
    @Operation(summary = "Resend temporary password", description = "Generates and sends a new temporary password to employee's email")
    public ResponseEntity<ApiResponseDTO<Void>> resendTemporaryPassword(
            @PathVariable Long employeeId,
            HttpServletRequest httpRequest) {

        employeeManagementService.resendTemporaryPassword(employeeId);

        ApiResponseDTO<Void> apiResponse = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Temporary password sent successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
