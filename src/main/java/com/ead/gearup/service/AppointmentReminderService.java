package com.ead.gearup.service;

import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    /**
     * Send reminder emails for appointments tomorrow
     * Runs every day at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDailyReminders() {
        log.info("Starting daily appointment reminder task...");
        
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        List<Appointment> appointments = appointmentRepository.findAll().stream()
                .filter(apt -> apt.getDate().equals(tomorrow))
                .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED 
                            || apt.getStatus() == AppointmentStatus.IN_PROGRESS)
                .toList();
        
        log.info("Found {} appointments for tomorrow ({})", appointments.size(), tomorrow);
        
        for (Appointment appointment : appointments) {
            try {
                String customerEmail = appointment.getCustomer().getUser().getEmail();
                String customerName = appointment.getCustomer().getUser().getName();
                String vehicleInfo = appointment.getVehicle().getMake() + " " + 
                                   appointment.getVehicle().getModel() + " (" + 
                                   appointment.getVehicle().getLicensePlate() + ")";
                String appointmentTime = appointment.getStartTime() != null 
                        ? appointment.getStartTime().toString() 
                        : "Not specified";
                
                emailService.sendAppointmentReminderEmail(
                        customerEmail, 
                        customerName, 
                        appointment.getDate().toString(), 
                        appointmentTime,
                        vehicleInfo
                );
                
                log.info("Sent daily reminder to {} for appointment on {}", customerEmail, tomorrow);
            } catch (Exception e) {
                log.error("Failed to send daily reminder for appointment {}: {}", 
                         appointment.getAppointmentId(), e.getMessage());
            }
        }
        
        log.info("Completed daily appointment reminder task");
    }

    /**
     * Send reminder emails for appointments starting in 1 hour
     * Runs every 30 minutes during business hours (8 AM - 6 PM)
     */
    @Scheduled(cron = "0 */30 8-18 * * *")
    @Transactional(readOnly = true)
    public void sendHourlyReminders() {
        log.info("Starting hourly appointment reminder task...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        
        LocalDate today = LocalDate.now();
        LocalTime reminderStartTime = now.toLocalTime();
        LocalTime reminderEndTime = oneHourLater.toLocalTime();
        
        List<Appointment> appointments = appointmentRepository.findAll().stream()
                .filter(apt -> apt.getDate().equals(today))
                .filter(apt -> apt.getStartTime() != null)
                .filter(apt -> {
                    LocalTime startTime = apt.getStartTime();
                    return !startTime.isBefore(reminderStartTime) && startTime.isBefore(reminderEndTime);
                })
                .filter(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED 
                            || apt.getStatus() == AppointmentStatus.IN_PROGRESS)
                .toList();
        
        log.info("Found {} appointments starting within the next hour", appointments.size());
        
        for (Appointment appointment : appointments) {
            try {
                String customerEmail = appointment.getCustomer().getUser().getEmail();
                String customerName = appointment.getCustomer().getUser().getName();
                String vehicleInfo = appointment.getVehicle().getMake() + " " + 
                                   appointment.getVehicle().getModel() + " (" + 
                                   appointment.getVehicle().getLicensePlate() + ")";
                String appointmentTime = appointment.getStartTime().toString();
                
                emailService.sendAppointmentUrgentReminderEmail(
                        customerEmail, 
                        customerName, 
                        appointmentTime,
                        vehicleInfo
                );
                
                log.info("Sent urgent reminder to {} for appointment at {}", 
                        customerEmail, appointmentTime);
            } catch (Exception e) {
                log.error("Failed to send urgent reminder for appointment {}: {}", 
                         appointment.getAppointmentId(), e.getMessage());
            }
        }
        
        log.info("Completed hourly appointment reminder task");
    }
}
