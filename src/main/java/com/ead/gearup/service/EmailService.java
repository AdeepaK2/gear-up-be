package com.ead.gearup.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.ead.gearup.exception.EmailSendingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendVerificationEmail(String to, String name, String verificationUrl) {
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

        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    public void sendEmployeeCredentials(String to, String name, String temporaryPassword, 
                                       String role, String specialization) {
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
            context.setVariable("loginUrl", "http://localhost:3000/login");

            // Generate HTML content from template
            String htmlContent = templateEngine.process("employee-credentials.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send employee credentials email: " + e.getMessage(), e);
        }
    }

    public void sendEmployeePasswordReset(String to, String name, String temporaryPassword) {
        try {
            String subject = "Your Temporary Password - Gear Up";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("loginUrl", "http://localhost:3000/login");

            // Generate HTML content from template
            String htmlContent = templateEngine.process("employee-password-reset.html", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
}
