package com.ead.gearup.model;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double hoursWorked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @PrePersist
    public void prePersist() {
        this.loggedAt = LocalDateTime.now();
        validateAndCalculate();
    }

    @PreUpdate
    public void preUpdate() {
        validateAndCalculate();
    }

    private void validateAndCalculate() {
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("End time must be after start time");
            }

            long seconds = Duration.between(startTime, endTime).getSeconds();
            double rawHours = seconds / 3600.0;

            this.hoursWorked = Math.round(rawHours * 100.0) / 100.0;
        } else {
            this.hoursWorked = 0.0;
        }
    }
}