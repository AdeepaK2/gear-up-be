package com.ead.gearup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.user.PasswordChangeRequest;
import com.ead.gearup.dto.user.PasswordChangeResponse;
import com.ead.gearup.dto.response.ApiResponseDTO;
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
    public ResponseEntity<ApiResponseDTO<PasswordChangeResponse>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        PasswordChangeResponse response = userService.changePassword(userEmail, request);

        return ResponseEntity.ok(ApiResponseDTO.<PasswordChangeResponse>builder()
                .status("success")
                .message("Password changed successfully")
                .data(response)
                .build());
    }

    @GetMapping("/password-status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'CUSTOMER')")
    @Operation(summary = "Check if password change is required", description = "Check if user needs to change their password")
    public ResponseEntity<ApiResponseDTO<PasswordChangeResponse>> getPasswordStatus(Authentication authentication) {
        String userEmail = authentication.getName();
        boolean requiresChange = userService.requiresPasswordChange(userEmail);

        return ResponseEntity.ok(ApiResponseDTO.<PasswordChangeResponse>builder()
                .status("success")
                .message("Password status retrieved")
                .data(new PasswordChangeResponse(
                    requiresChange ? "Password change required" : "Password is up to date",
                    requiresChange
                ))
                .build());
    }
}
