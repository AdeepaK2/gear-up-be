package com.ead.gearup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.ChangePasswordRequest;
import com.ead.gearup.dto.PasswordChangeResponse;
import com.ead.gearup.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User account management operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Change user password", description = "Allows authenticated users to change their password")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        PasswordChangeResponse response = userService.changePassword(userEmail, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Password changed successfully"));
    }

    @GetMapping("/password-status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Check if password change is required", description = "Check if user needs to change their password")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> getPasswordStatus(Authentication authentication) {
        String userEmail = authentication.getName();
        boolean requiresChange = userService.requiresPasswordChange(userEmail);
        
        return ResponseEntity.ok(ApiResponse.success(
            new PasswordChangeResponse(
                requiresChange ? "Password change required" : "Password is up to date",
                requiresChange
            ),
            "Password status retrieved"
        ));
    }

    // Inner class for API response
    private static class ApiResponse<T> {
        private String status;
        private String message;
        private T data;
        private String timestamp;

        public static <T> ApiResponse<T> success(T data, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.status = "success";
            response.message = message;
            response.data = data;
            response.timestamp = java.time.LocalDateTime.now().toString();
            return response;
        }

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
