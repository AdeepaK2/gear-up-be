package com.ead.gearup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "closed_dates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "closed_date", nullable = false)
    private LocalDate closedDate;
    
    @Column(name = "reason")
    private String reason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_settings_id")
    private ShopSettings shopSettings;
}
