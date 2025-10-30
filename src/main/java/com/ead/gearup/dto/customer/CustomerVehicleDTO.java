package com.ead.gearup.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVehicleDTO {
    private Long id;
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private String nextService;
}
