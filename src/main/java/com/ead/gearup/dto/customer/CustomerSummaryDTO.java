package com.ead.gearup.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSummaryDTO {
    private long upcomingAppointmentsCount;
    private String nextAppointmentDate;
    private long ongoingProjectsCount;
    private String ongoingProjectStatus;
    private long completedServicesCount;
    private long pendingRequestsCount;
}
