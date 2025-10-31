package com.ead.gearup.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateProjectDTO {

    @NotBlank
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    private Long appointmentId;

    @NotNull
    private Long vehicleId;

    @NotNull
    private List<Long> taskIds;


}
