package com.ead.gearup.controller;

import com.ead.gearup.dto.customer.*;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.service.VehicleService;
import com.ead.gearup.validation.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer API", description = "CRUD operations for customers")
public class CustomerController {

    private final CustomerService customerService;
    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Get all customers")
//     @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public ResponseEntity<ApiResponseDTO<List<CustomerResponseDTO>>> getAll(HttpServletRequest request) {
        List<CustomerResponseDTO> customers = customerService.getAll();

        ApiResponseDTO<List<CustomerResponseDTO>> response = ApiResponseDTO.<List<CustomerResponseDTO>>builder()
                .status("success")
                .message("Customers retrieved successfully")
                .data(customers)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
//     @RequiresRole({ UserRole.CUSTOMER, UserRole.ADMIN, UserRole.EMPLOYEE })
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> getById(@PathVariable Long id,
            HttpServletRequest request) {
        CustomerResponseDTO customer = customerService.getById(id);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer retrieved successfully")
                .data(customer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create new customer linked to the logged-in user")
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> create(
            @Valid @RequestBody CustomerRequestDTO dto,
            HttpServletRequest request) {

        CustomerResponseDTO createdCustomer = customerService.create(dto);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer created successfully")
                .data(createdCustomer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update customer by ID")
    @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<ApiResponseDTO<CustomerResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateDTO dto,
            HttpServletRequest request) {

        CustomerResponseDTO updatedCustomer = customerService.update(id, dto);

        ApiResponseDTO<CustomerResponseDTO> response = ApiResponseDTO.<CustomerResponseDTO>builder()
                .status("success")
                .message("Customer updated successfully")
                .data(updatedCustomer)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer by ID")
    @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<ApiResponseDTO<Object>> delete(@PathVariable Long id, HttpServletRequest request) {
        customerService.delete(id);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Customer deleted successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    //Header/GetProfile
    @GetMapping("/{id}/header")
    @RequiresRole(UserRole.CUSTOMER)
    @Operation(summary = "Get customer header info (name & profile image)")
    public ResponseEntity<ApiResponseDTO<CustomerHeaderDTO>> getHeaderInfo(
            @PathVariable Long id,
            HttpServletRequest request) {

        CustomerHeaderDTO header = customerService.getHeaderInfo(id);

        ApiResponseDTO<CustomerHeaderDTO> response = ApiResponseDTO.<CustomerHeaderDTO>builder()
                .status("success")
                .message("Customer header info retrieved successfully")
                .data(header)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

      //Header/Get Notification
//    @GetMapping("/{id}/notifications")
//    @Operation(summary = "Get notifications for a customer")
//    public ResponseEntity<ApiResponseDTO<List<NotificationDTO>>> getNotifications(
//            @PathVariable Long id,
//            HttpServletRequest request) {
//
//        List<NotificationDTO> notifications = customerService.getNotifications(id);
//
//        ApiResponseDTO<List<NotificationDTO>> response = ApiResponseDTO.<List<NotificationDTO>>builder()
//                .status("success")
//                .message("Customer notifications retrieved successfully")
//                .data(notifications)
//                .timestamp(Instant.now())
//                .path(request.getRequestURI())
//                .build();
//
//        return ResponseEntity.ok(response);
//    }

    //Customer Dashboard
    @GetMapping("/{id}/dashboard")
    @RequiresRole(UserRole.CUSTOMER)
    @Operation(summary = "Get full customer dashboard details")
    public ResponseEntity<ApiResponseDTO<CustomerDashboardDTO>> getDashboard(
            @PathVariable Long id,
            HttpServletRequest request) {

        CustomerDashboardDTO dashboard = customerService.getDashboard(id);

        ApiResponseDTO<CustomerDashboardDTO> response = ApiResponseDTO.<CustomerDashboardDTO>builder()
                .status("success")
                .message("Customer dashboard retrieved successfully")
                .data(dashboard)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

}


