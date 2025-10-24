package com.ead.gearup.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDTO {

    private Long id;

    private Long vehicleId;
    private String vehicleName;
    private String vehicleDetails;

    private Long customerId;
    private Long employeeId;

    private String consultationType;
    private String consultationTypeLabel;

    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String status;
    private String customerIssue;
    private String notes;

    private List<Long> taskIds;
}
