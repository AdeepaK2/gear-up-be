package com.ead.gearup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;
    
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;
    
    // Store as comma-separated string: "1,2,3,4,5" for Mon-Fri
    @Column(name = "operating_days", nullable = false)
    private String operatingDays;
    
    @Column(name = "is_shop_open", nullable = false)
    private Boolean isShopOpen = true;
    
    @OneToMany(mappedBy = "shopSettings", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ClosedDate> closedDates = new ArrayList<>();
    
    // Helper methods to convert between String and List<Integer>
    public List<Integer> getOperatingDaysList() {
        if (operatingDays == null || operatingDays.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> days = new ArrayList<>();
        for (String day : operatingDays.split(",")) {
            days.add(Integer.parseInt(day.trim()));
        }
        return days;
    }
    
    public void setOperatingDaysList(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            this.operatingDays = "";
        } else {
            this.operatingDays = String.join(",", days.stream().map(String::valueOf).toArray(String[]::new));
        }
    }
}
