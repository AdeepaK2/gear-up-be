package com.ead.gearup.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.request.ForgotPasswordRequestDTO;
import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.request.ResetPasswordRequestDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.PasswordChangeRequest;
import com.ead.gearup.dto.user.PasswordChangeResponse;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.service.AuthService;
import com.ead.gearup.service.UserService;
import com.ead.gearup.service.auth.JwtService;
import com.ead.gearup.validation.RequiresRole;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authService;
    private final UserService userService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new user", description = "Creates a new user account. After registration, a verification email will be sent to the provided email address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class), examples = @ExampleObject(value = """
                    {
                        "status": "success",
                        "message": "User registered successfully! Please verify your email to activate your account.",
                        "data": {
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "firstName": "John",
                            "lastName": "Doe",
                            "email": "john.doe@example.com",
                            "role": "CUSTOMER",
                            "isEmailVerified": false
                        },
                        "timestamp": "2023-10-15T10:30:00Z",
                        "path": "/api/v1/auth/register"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or email already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(
            @Valid @RequestBody @Parameter(description = "User registration details", required = true) UserCreateDTO userCreateDTO,
            HttpServletRequest request) {

        UserResponseDTO createdUser = authService.createUser(userCreateDTO);

        ApiResponseDTO<UserResponseDTO> apiResponse = ApiResponseDTO.<UserResponseDTO>builder()
                .status("success")
                .message("User registered successfully! Please verify your email to activate your account.")
                .data(createdUser)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Verifies a user's email address using the verification token sent via email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<Object>> verifyEmail(
            @RequestParam("token") @Parameter(description = "Email verification token", required = true, example = "eyJhbGciOiJIUzI1NiJ9...") String token,
            HttpServletRequest request) {

        boolean verified = authService.verifyEmailToken(token);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status(verified ? "success" : "error")
                .message(verified ? "Email verified successfully!" : "Invalid or expired verification token")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(null)
                .build();

        return verified
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/resend-email")
    @Operation(summary = "Resend verification email", description = "Resends the email verification link to the user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email resent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email or user not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<Object>> resendEmail(
            @Valid @RequestBody @Parameter(description = "Email address to resend verification", required = true) ResendEmailRequestDTO resendEmailRequestDTO,
            HttpServletRequest httpRequest) {

        authService.resendEmail(resendEmailRequestDTO);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Verification email resent successfully")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> verifyUser(
            @Valid @RequestBody UserLoginDTO userLoginDTO,
            HttpServletRequest request) {

        JwtTokensDTO tokens = authService.verifyUser(userLoginDTO);

        // HttpOnly refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofMillis(jwtService.getRefreshTokenDurationMs()))
                .sameSite("None")
                .build();

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setAccessToken(tokens.getAccessToken());

        ApiResponseDTO<LoginResponseDTO> apiResponse = ApiResponseDTO.<LoginResponseDTO>builder()
                .status("success")
                .message("Login successful")
                .data(loginResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponseDTO loginResponse = authService.getRefreshAccessToken(refreshToken);

        ApiResponseDTO<LoginResponseDTO> apiResponse = ApiResponseDTO.<LoginResponseDTO>builder()
                .status("success")
                .message("Token refreshed successfully")
                .data(loginResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Object>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                    .status("error")
                    .message("No active session found or already logged out")
                    .data(null)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Clear the refresh token cookie
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite("None")
                .build();

        ApiResponseDTO<Object> apiResponse = ApiResponseDTO.builder()
                .status("success")
                .message("Logged out successfully")
                .data(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(apiResponse);
    }

    @PostMapping("/change-password")
    @RequiresRole({ UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN })
    @Operation(summary = "Change user password", description = "Allows authenticated users to change their password")
    public ResponseEntity<ApiResponseDTO<PasswordChangeResponse>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String userEmail = authentication.getName();
        PasswordChangeResponse response = userService.changePassword(userEmail, request);

        ApiResponseDTO<PasswordChangeResponse> apiResponse = ApiResponseDTO.<PasswordChangeResponse>builder()
                .status("success")
                .message("Password changed successfully")
                .data(response)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/password-status")
    @RequiresRole({ UserRole.CUSTOMER, UserRole.EMPLOYEE, UserRole.ADMIN })
    @Operation(summary = "Check if password change is required", description = "Check if user needs to change their password")
    public ResponseEntity<ApiResponseDTO<PasswordChangeResponse>> getPasswordStatus(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String userEmail = authentication.getName();
        boolean requiresChange = userService.requiresPasswordChange(userEmail);

        PasswordChangeResponse response = new PasswordChangeResponse(
                requiresChange ? "Password change required" : "Password is up to date",
                requiresChange);

        ApiResponseDTO<PasswordChangeResponse> apiResponse = ApiResponseDTO.<PasswordChangeResponse>builder()
                .status("success")
                .message("Password status retrieved")
                .data(response)
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends a password reset link to the user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<Object>> forgotPassword(
            @Valid @RequestBody @Parameter(description = "Email address for password reset", required = true) ForgotPasswordRequestDTO request,
            HttpServletRequest httpRequest) {

        authService.forgotPassword(request);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Password reset link has been sent to your email")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using the token from email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<Object>> resetPassword(
            @Valid @RequestBody @Parameter(description = "Password reset details", required = true) ResetPasswordRequestDTO request,
            HttpServletRequest httpRequest) {

        authService.resetPassword(request);

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Password has been reset successfully")
                .timestamp(Instant.now())
                .path(httpRequest.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
