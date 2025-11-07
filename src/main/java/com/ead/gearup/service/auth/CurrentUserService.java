package com.ead.gearup.service.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.AccessDeniedException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrinciple userDetails) {
            return userDetails.getUserId();
        }

        throw new AccessDeniedException("Unauthorized");
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get the current user's role
     */
    public UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /*
     * Get the current user's role-specific entity ID
     * The entity ID (customerId, employeeId, or userId for admin),
     * or null if not found
     */
    public Long getCurrentEntityId() {
        Long userId = getCurrentUserId();
        log.debug("Getting entity ID for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() == null) {
            log.warn("User {} has no role assigned", userId);
            return null;
        }

        log.debug("User {} has role: {}", userId, user.getRole());

        return switch (user.getRole()) {
            case CUSTOMER -> {
                var customerOpt = customerRepository.findByUser(user);
                if (customerOpt.isEmpty()) {
                    log.warn("Customer entity not found for user {} with email {}", userId, user.getEmail());
                    yield null;
                } else {
                    Long customerId = customerOpt.get().getCustomerId();
                    log.debug("Found customer entity with ID: {}", customerId);
                    yield customerId;
                }
            }
            case EMPLOYEE -> {
                var employeeOpt = employeeRepository.findByUser(user);
                if (employeeOpt.isEmpty()) {
                    log.warn("Employee entity not found for user {} with email {}", userId, user.getEmail());
                    yield null;
                } else {
                    Long employeeId = employeeOpt.get().getEmployeeId();
                    log.debug("Found employee entity with ID: {}", employeeId);
                    yield employeeId;
                }
            }
            case ADMIN -> {
                log.debug("Admin user, returning user ID: {}", user.getUserId());
                yield user.getUserId();
            }
            case PUBLIC -> {
                log.debug("Public user, returning user ID: {}", user.getUserId());
                yield user.getUserId();
            }
        };
    }

    public UserRole getCurrentUserType() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (user.getRole() == null) {
            throw new RuntimeException("User role is not set");
        }

        return user.getRole();
    }

}
