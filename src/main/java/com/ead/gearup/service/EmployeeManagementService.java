package com.ead.gearup.service;

import com.ead.gearup.dto.CreateEmployeeRequest;
import com.ead.gearup.dto.CreateEmployeeResponse;
import com.ead.gearup.dto.EmployeeDTO;
import com.ead.gearup.exception.EmailAlreadyExistsException;
import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest request) {
        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();

        User employee;

        // Check if user with this email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            // Try to find and reactivate existing user if they're inactive or not an employee
            User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailAlreadyExistsException(request.getEmail()));

            // Only allow reactivation if the user is not currently an active employee
            if (existingUser.getRole() == UserRole.EMPLOYEE && existingUser.getIsActive()) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }

            // Reactivate and update the existing user
            log.info("Reactivating existing user {} as employee", request.getEmail());
            employee = existingUser;
            employee.setName(request.getName());
            employee.setPassword(passwordEncoder.encode(temporaryPassword));
            employee.setRole(UserRole.EMPLOYEE);
            employee.setIsVerified(true);
            employee.setIsActive(true);
            employee.setRequiresPasswordChange(true);
        } else {
            // Create new employee user
            employee = new User();
            employee.setEmail(request.getEmail());
            employee.setName(request.getName());
            employee.setPassword(passwordEncoder.encode(temporaryPassword));
            employee.setRole(UserRole.EMPLOYEE);
            employee.setIsVerified(true); // Auto-verify employee accounts
            employee.setIsActive(true);
            employee.setRequiresPasswordChange(true); // Force password change on first login
            employee.setCreatedAt(LocalDateTime.now());
        }

        // Set additional employee-specific fields if your User entity has them
        // employee.setEmployeeRole(request.getRole());
        // employee.setSpecialization(request.getSpecialization());

        User savedEmployee = userRepository.save(employee);

        // Send email with temporary credentials
        try {
            emailService.sendEmployeeCredentials(
                savedEmployee.getEmail(),
                savedEmployee.getName(),
                temporaryPassword,
                request.getRole(),
                request.getSpecialization()
            );
        } catch (Exception e) {
            log.error("Failed to send employee credentials email to {}", savedEmployee.getEmail(), e);
            // Don't fail the request if email fails, log it instead
        }

        // Build response
        EmployeeDTO employeeDTO = EmployeeDTO.builder()
                .id(savedEmployee.getUserId())
                .name(savedEmployee.getName())
                .email(savedEmployee.getEmail())
                .role(request.getRole())
                .specialization(request.getSpecialization())
                .createdAt(savedEmployee.getCreatedAt())
                .isActive(savedEmployee.getIsActive())
                .build();

        return CreateEmployeeResponse.builder()
                .employee(employeeDTO)
                .temporaryPassword(temporaryPassword)
                .message("Employee account created. Login credentials sent to " + savedEmployee.getEmail())
                .build();
    }

    public List<EmployeeDTO> getAllEmployees() {
        return userRepository.findByRole(UserRole.EMPLOYEE).stream()
                .filter(user -> user.getIsActive()) // Only show active employees
                .map(user -> EmployeeDTO.builder()
                        .id(user.getUserId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role("Employee") // You can store this in User entity
                        .specialization("General") // You can store this in User entity
                        .createdAt(user.getCreatedAt())
                        .isActive(user.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateEmployee(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!employee.getRole().equals(UserRole.EMPLOYEE)) {
            throw new RuntimeException("User is not an employee");
        }

        employee.setIsActive(false);
        userRepository.save(employee);
    }

    @Transactional
    public void reactivateEmployee(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!employee.getRole().equals(UserRole.EMPLOYEE)) {
            throw new RuntimeException("User is not an employee");
        }

        employee.setIsActive(true);
        userRepository.save(employee);
    }

    @Transactional
    public void resendTemporaryPassword(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!employee.getRole().equals(UserRole.EMPLOYEE)) {
            throw new RuntimeException("User is not an employee");
        }

        // Generate new temporary password
        String newTemporaryPassword = generateTemporaryPassword();
        employee.setPassword(passwordEncoder.encode(newTemporaryPassword));
        userRepository.save(employee);

        // Send email with new credentials
        try {
            emailService.sendEmployeePasswordReset(
                employee.getEmail(),
                employee.getName(),
                newTemporaryPassword
            );
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", employee.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        // Ensure at least one uppercase, lowercase, digit, and special char
        return ensurePasswordComplexity(password.toString());
    }

    private String ensurePasswordComplexity(String password) {
        char[] chars = password.toCharArray();
        // Ensure at least one uppercase
        if (!password.matches(".*[A-Z].*")) {
            chars[0] = (char) ('A' + random.nextInt(26));
        }
        // Ensure at least one lowercase
        if (!password.matches(".*[a-z].*")) {
            chars[1] = (char) ('a' + random.nextInt(26));
        }
        // Ensure at least one digit
        if (!password.matches(".*[0-9].*")) {
            chars[2] = (char) ('0' + random.nextInt(10));
        }
        // Ensure at least one special char
        if (!password.matches(".*[@#$].*")) {
            chars[3] = '@';
        }
        return new String(chars);
    }
}
