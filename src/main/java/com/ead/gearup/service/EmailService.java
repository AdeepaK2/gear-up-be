package com.ead.gearup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.ead.gearup.exception.EmailSendingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.verification.enabled:false}")
    private boolean emailVerificationEnabled;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(String to, String name, String verificationUrl) {
        // For development: Skip actual email sending if disabled
        if (!emailVerificationEnabled) {
            log.info("📧 Email verification DISABLED for development");
            log.info("📧 Verification email would be sent to: {}", to);
            log.info("📧 Verification URL: {}", verificationUrl);
            log.info("📧 User '{}' can proceed without email verification", name);
            return;
        }

        try {
            String subject = "Verify Your Email";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationUrl", verificationUrl);

            // Generate HTML content from template
            String htmlContent = templateEngine.process("verification-email.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("📧 Verification email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send verification email to {}: {}", to, e.getMessage());
            throw new EmailSendingException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    public void sendEmployeeCredentials(String to, String name, String temporaryPassword, 
                                       String role, String specialization) {
        // For development: Skip actual email sending if disabled
        if (!emailVerificationEnabled) {
            log.info("📧 Email sending DISABLED for development");
            log.info("📧 Employee credentials email would be sent to: {}", to);
            log.info("📧 Temporary password: {}", temporaryPassword);
            return;
        }

        try {
            String subject = "Welcome to Gear Up - Your Employee Account";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("role", role);
            context.setVariable("specialization", specialization);
            context.setVariable("loginUrl", frontendUrl + "/login");

            // Generate HTML content from template
            String htmlContent = templateEngine.process("employee-credentials.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("📧 Employee credentials email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send employee credentials email to {}: {}", to, e.getMessage());
            throw new EmailSendingException("Failed to send employee credentials email: " + e.getMessage(), e);
        }
    }

    public void sendEmployeePasswordReset(String to, String name, String temporaryPassword) {
        // For development: Skip actual email sending if disabled
        if (!emailVerificationEnabled) {
            log.info("📧 Email sending DISABLED for development");
            log.info("📧 Password reset email would be sent to: {}", to);
            log.info("📧 Temporary password: {}", temporaryPassword);
            return;
        }

        try {
            String subject = "Your Temporary Password - Gear Up";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("loginUrl", frontendUrl + "/login");

            // Generate HTML content from template
            String htmlContent = templateEngine.process("employee-password-reset.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("📧 Password reset email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to {}: {}", to, e.getMessage());
            throw new EmailSendingException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public void sendCustomerDeactivationEmail(String to, String name, String reason) {
        // For development: Skip actual email sending if disabled
        if (!emailVerificationEnabled) {
            log.info("📧 Email sending DISABLED for development");
            log.info("📧 Customer deactivation email would be sent to: {}", to);
            log.info("📧 Reason: {}", reason);
            return;
        }

        try {
            String subject = "Your Account Has Been Temporarily Suspended - Gear Up";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("reason", reason);
            context.setVariable("supportEmail", "support@gearup.com");
            context.setVariable("supportPhone", "+94 11 234 5678");

            // Generate HTML content from template
            String htmlContent = templateEngine.process("customer-deactivation.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("📧 Customer deactivation email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send customer deactivation email to {}: {}", to, e.getMessage());
            throw new EmailSendingException("Failed to send customer deactivation email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String to, String name, String resetUrl) {
        // For development: Skip actual email sending if disabled
        if (!emailVerificationEnabled) {
            log.info("📧 Email sending DISABLED for development");
            log.info("📧 Password reset email would be sent to: {}", to);
            log.info("📧 Reset URL: {}", resetUrl);
            return;
        }

        try {
            String subject = "Reset Your Password - Gear Up";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetUrl", resetUrl);

            // Generate HTML content from template
            String htmlContent = templateEngine.process("password-reset.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("📧 Password reset email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to {}: {}", to, e.getMessage());
            throw new EmailSendingException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
}
