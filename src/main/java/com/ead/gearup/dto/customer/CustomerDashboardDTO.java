package com.ead.gearup.dto.customer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDashboardDTO {
    private CustomerProfileDTO profile;
    private CustomerSummaryDTO summary;
//    private List<CustomerNotificationDTO> notifications;
    private List<CustomerActivityDTO> recentActivities;
    private List<CustomerVehicleDTO> vehicles;
}

