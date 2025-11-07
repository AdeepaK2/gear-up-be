package com.ead.gearup.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminStatsDTO {
    long totalEmployees;
    long activeProjects;
    long upcomingAppointments;
    long totalCustomers;
    long totalServices;
}

