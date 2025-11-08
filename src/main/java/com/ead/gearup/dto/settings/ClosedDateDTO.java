package com.ead.gearup.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDateDTO {
    
    @NotBlank(message = "Date is required")
    private String date; // ISO format: yyyy-MM-dd
    
    private String reason;
}
