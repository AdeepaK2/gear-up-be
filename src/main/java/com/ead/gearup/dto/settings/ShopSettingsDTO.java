package com.ead.gearup.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSettingsDTO {
    
    private Long id;
    
    // Operating hours
    private LocalTime openingTime;
    private LocalTime closingTime;
    
    // Operating days (0 = Sunday, 1 = Monday, ... 6 = Saturday)
    private List<Integer> operatingDays;
    
    // Shop status
    private Boolean isShopOpen;
    
    // Closed dates (for holidays, maintenance, etc.)
    private List<String> closedDates; // ISO format: yyyy-MM-dd
}
