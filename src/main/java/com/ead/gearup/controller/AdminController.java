package com.ead.gearup.controller;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.admin.AdminDashboardResponseDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.AdminDashboardService;

import java.util.List;
import java.util.stream.Collectors;

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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Management", description = "Admin-specific operations and user management")
public class AdminController {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminDashboardService adminDashboardService;
    private final com.ead.gearup.service.ShopSettingsService shopSettingsService;

    @GetMapping("/check-init")
    @Operation(
        summary = "Check if admin account exists",
        description = "Returns whether the default admin account has been initialized. Useful for setup checks."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Admin check completed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                        "status": "success",
                        "message": "Admin account exists",
                        "data": {
                            "adminExists": true,
                            "adminEmail": "admin@gearup.com"
                        },
                        "timestamp": "2023-10-15T10:30:00Z",
                        "path": "/api/v1/admin/check-init"
                    }
                    """)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<Object>> checkAdminInit(HttpServletRequest request) {
        boolean adminExists = userRepository.findByEmail("admin@gearup.com").isPresent();

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message(adminExists ? "Admin account exists" : "Admin account not initialized")
                .data(new AdminCheckResponse(adminExists, adminExists ? "admin@gearup.com" : null))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/create-admin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create additional admin account",
        description = "Creates a new admin user. This endpoint can be used to add more admins to the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Admin created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                        "status": "success",
                        "message": "Admin user created successfully",
                        "data": {
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "firstName": "John",
                            "lastName": "Admin",
                            "email": "john.admin@gearup.com",
                            "role": "ADMIN",
                            "isEmailVerified": true
                        },
                        "timestamp": "2023-10-15T10:30:00Z",
                        "path": "/api/v1/admin/create-admin"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or email already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Requires ADMIN role",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createAdmin(
            @Valid @RequestBody 
            @Parameter(description = "Admin user details", required = true)
            UserCreateDTO userCreateDTO,
            HttpServletRequest request) {

        // Check if user already exists
        if (userRepository.findByEmail(userCreateDTO.getEmail()).isPresent()) {
            ApiResponseDTO<UserResponseDTO> errorResponse = ApiResponseDTO.<UserResponseDTO>builder()
                    .status("error")
                    .message("User with this email already exists")
                    .data(null)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Create admin user
        User admin = User.builder()
                .email(userCreateDTO.getEmail())
                .name(userCreateDTO.getName())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .role(UserRole.ADMIN)
                .isVerified(true) // Admins are pre-verified
                .build();

        User savedAdmin = userRepository.save(admin);

        // Create response
        UserResponseDTO userResponse = new UserResponseDTO(
                savedAdmin.getEmail(),
                savedAdmin.getName()
        );

        ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.<UserResponseDTO>builder()
                .status("success")
                .message("Admin user created successfully")
                .data(userResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/dashboard")
    @Operation(
        summary = "Retrieve admin dashboard metrics",
        description = "Provides aggregated metrics and time series data for the admin dashboard."
    )
    public ResponseEntity<ApiResponseDTO<AdminDashboardResponseDTO>> getDashboard(HttpServletRequest request) {
        AdminDashboardResponseDTO dashboard = adminDashboardService.getDashboard();

        ApiResponseDTO<AdminDashboardResponseDTO> response = ApiResponseDTO.<AdminDashboardResponseDTO>builder()
                .status("success")
                .message("Dashboard data retrieved successfully")
                .data(dashboard)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/employees", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @Operation(
        summary = "Create employee account (Admin only)",
        description = "Creates a new employee user and employee record. This endpoint allows admins to create employee accounts directly."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Employee created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or email already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createEmployee(
            @Parameter(description = "Employee creation data", required = true)
            @Valid @RequestBody UserCreateDTO userCreateDTO,
            HttpServletRequest request) {

        // Check if email already exists
        if (userRepository.findByEmail(userCreateDTO.getEmail()).isPresent()) {
            ApiResponseDTO<UserResponseDTO> errorResponse = ApiResponseDTO.<UserResponseDTO>builder()
                    .status("error") 
                    .message("Email address already exists")
                    .data(null)
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Create user with EMPLOYEE role
        User employee = User.builder()
                .email(userCreateDTO.getEmail())
                .name(userCreateDTO.getName())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .role(UserRole.EMPLOYEE)
                .isVerified(true) // Employees created by admin are pre-verified
                .build();

        User savedEmployee = userRepository.save(employee);

        // Create Employee entity linked to the User
        Employee employeeEntity = Employee.builder()
                .user(savedEmployee)
                .specialization("General") // Default specialization
                .hireDate(LocalDate.now())
                .build();

        employeeRepository.save(employeeEntity);

        // Create response
        UserResponseDTO userResponse = new UserResponseDTO(
                savedEmployee.getEmail(),
                savedEmployee.getName()
        );

        ApiResponseDTO<UserResponseDTO> response = ApiResponseDTO.<UserResponseDTO>builder()
                .status("success")
                .message("Employee user and profile created successfully")
                .data(userResponse)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/migrate-customers")
    @Operation(
        summary = "Migrate existing users to create missing Customer records",
        description = "Creates Customer entities for all users with CUSTOMER role who don't have a Customer record. This is a one-time migration endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Migration completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<Object>> migrateCustomers(HttpServletRequest request) {
        // Find all users with CUSTOMER role who don't have a Customer entity
        List<User> usersWithoutCustomer = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .filter(user -> customerRepository.findByUser(user).isEmpty())
                .collect(Collectors.toList());

        int migratedCount = 0;
        for (User user : usersWithoutCustomer) {
            Customer customer = Customer.builder()
                    .user(user)
                    .phoneNumber(null) // Can be updated later in profile
                    .build();
            customerRepository.save(customer);
            migratedCount++;
        }

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Migration completed. Created " + migratedCount + " Customer records.")
                .data(new MigrationResponse(migratedCount, usersWithoutCustomer.size()))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/migrate-employees")
    @Operation(
        summary = "Migrate existing EMPLOYEE users to create missing Employee records",
        description = "Creates Employee entities for all users with EMPLOYEE role who don't have an Employee record. This fixes the missing employee record issue."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Migration completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    @Transactional
    public ResponseEntity<ApiResponseDTO<Object>> migrateEmployees(HttpServletRequest request) {
        // Find all users with EMPLOYEE role who don't have an Employee entity
        List<User> usersWithoutEmployee = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.EMPLOYEE)
                .filter(user -> employeeRepository.findByUser(user).isEmpty())
                .collect(Collectors.toList());

        int migratedCount = 0;
        for (User user : usersWithoutEmployee) {
            Employee employee = Employee.builder()
                    .user(user)
                    .specialization("General") // Default specialization
                    .hireDate(LocalDate.now()) // Default hire date to today
                    .build();
            employeeRepository.save(employee);
            migratedCount++;
        }

        ApiResponseDTO<Object> response = ApiResponseDTO.builder()
                .status("success")
                .message("Employee migration completed. Created " + migratedCount + " Employee records.")
                .data(new MigrationResponse(migratedCount, usersWithoutEmployee.size()))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    // ==================== Shop Settings Endpoints ====================

    @GetMapping("/settings")
    @Operation(
        summary = "Get shop settings",
        description = "Retrieve current shop operating hours, days, and closed dates"
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
    public ResponseEntity<ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO>> getShopSettings(
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

    @PostMapping("/settings")
    @Operation(
        summary = "Update shop settings",
        description = "Update shop operating hours, days, and open/closed status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Shop settings updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO>> updateShopSettings(
            @Valid @RequestBody com.ead.gearup.dto.settings.UpdateShopSettingsDTO updateDTO,
            HttpServletRequest request) {
        
        com.ead.gearup.dto.settings.ShopSettingsDTO settings = shopSettingsService.updateShopSettings(updateDTO);

        ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO> response = 
            ApiResponseDTO.<com.ead.gearup.dto.settings.ShopSettingsDTO>builder()
                .status("success")
                .message("Shop settings updated successfully")
                .data(settings)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/settings/closed-dates")
    @Operation(
        summary = "Add closed date",
        description = "Add a date when the shop will be closed (holidays, maintenance, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Closed date added successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO>> addClosedDate(
            @Valid @RequestBody com.ead.gearup.dto.settings.ClosedDateDTO closedDateDTO,
            HttpServletRequest request) {
        
        com.ead.gearup.dto.settings.ShopSettingsDTO settings = shopSettingsService.addClosedDate(closedDateDTO);

        ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO> response = 
            ApiResponseDTO.<com.ead.gearup.dto.settings.ShopSettingsDTO>builder()
                .status("success")
                .message("Closed date added successfully")
                .data(settings)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/settings/closed-dates/remove")
    @Operation(
        summary = "Remove closed date",
        description = "Remove a previously added closed date"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Closed date removed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)
            )
        )
    })
    public ResponseEntity<ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO>> removeClosedDate(
            @RequestBody String date,
            HttpServletRequest request) {
        
        // Remove quotes if present
        String cleanDate = date.replaceAll("\"", "").trim();
        
        com.ead.gearup.dto.settings.ShopSettingsDTO settings = shopSettingsService.removeClosedDate(cleanDate);

        ApiResponseDTO<com.ead.gearup.dto.settings.ShopSettingsDTO> response = 
            ApiResponseDTO.<com.ead.gearup.dto.settings.ShopSettingsDTO>builder()
                .status("success")
                .message("Closed date removed successfully")
                .data(settings)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    // Inner class for admin check response
    private record AdminCheckResponse(boolean adminExists, String adminEmail) {}

    // Inner class for migration response
    private record MigrationResponse(int createdRecords, int totalUsersProcessed) {}
}
