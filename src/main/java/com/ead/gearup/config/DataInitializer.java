package com.ead.gearup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ead.gearup.enums.UserRole;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataInitializer - Creates default admin account on application startup
 * This ensures there's always at least one admin user in the system
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@gearup.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.init.enabled:true}")
    private boolean initEnabled;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            if (!initEnabled) {
                log.info("🔒 Admin initialization is disabled");
                return;
            }

            log.info("🚀 Starting database initialization...");

            // Check if admin already exists
            if (userRepository.findByEmail(adminEmail).isPresent()) {
                log.info("✅ Admin user already exists: {}", adminEmail);
                return;
            }

            // Create default admin user
            try {
                User admin = User.builder()
                        .email(adminEmail)
                        .name(adminName)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .isVerified(true) // Admin is pre-verified
                        .build();

                userRepository.save(admin);

                log.info("✅ Default admin user created successfully!");
                log.info("📧 Admin Email: {}", adminEmail);
                log.info("🔑 Admin Password: {} (Please change this in production!)", adminPassword);
                log.info("⚠️  IMPORTANT: Change admin credentials after first login!");

            } catch (Exception e) {
                log.error("❌ Failed to create admin user: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize admin user", e);
            }
        };
    }
}
