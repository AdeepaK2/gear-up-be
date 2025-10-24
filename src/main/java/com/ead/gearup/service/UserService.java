package com.ead.gearup.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ead.gearup.dto.user.PasswordChangeRequest;
import com.ead.gearup.dto.user.PasswordChangeResponse;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PasswordChangeResponse changePassword(String userEmail, PasswordChangeRequest request) {
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        
        // Clear password change requirement flag
        user.setRequiresPasswordChange(false);
        
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userEmail);

        return new PasswordChangeResponse(
            "Password changed successfully",
            false
        );
    }

    public boolean requiresPasswordChange(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return user.getRequiresPasswordChange() != null && user.getRequiresPasswordChange();
    }
}
