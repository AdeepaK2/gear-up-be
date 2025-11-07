package com.ead.gearup.dto.admin;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDashboardResponseDTO {
    AdminStatsDTO stats;
    List<ProjectStatusDTO> projectStatus;
    List<MonthlyMetricDTO> customerRegistrations;
    List<MonthlyMetricDTO> projectCompletions;
}

