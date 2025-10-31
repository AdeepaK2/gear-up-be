package com.ead.gearup.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.service.auth.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(User user) {
        try {
            // Create UserDetails manually, no verification check
            UserDetails userDetails = new UserPrinciple(user);

            String token = jwtService.generateEmailVerificationToken(userDetails);

            String verificationUrl = appBaseUrl + "/api/v1/auth/verify-email?token=" + token;

            // Send email
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationUrl);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

}
