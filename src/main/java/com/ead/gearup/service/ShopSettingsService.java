package com.ead.gearup.service;

import com.ead.gearup.dto.settings.ClosedDateDTO;
import com.ead.gearup.dto.settings.ShopSettingsDTO;
import com.ead.gearup.dto.settings.UpdateShopSettingsDTO;
import com.ead.gearup.entity.ClosedDate;
import com.ead.gearup.entity.ShopSettings;
import com.ead.gearup.exception.ResourceNotFoundException;
import com.ead.gearup.repository.ClosedDateRepository;
import com.ead.gearup.repository.ShopSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopSettingsService {

    @Autowired
    private ShopSettingsRepository shopSettingsRepository;

    @Autowired
    private ClosedDateRepository closedDateRepository;

    /**
     * Get or create shop settings (singleton pattern)
     */
    public ShopSettingsDTO getShopSettings() {
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> createDefaultSettings());
        
        return convertToDTO(settings);
    }

    /**
     * Update shop settings
     */
    @Transactional
    public ShopSettingsDTO updateShopSettings(UpdateShopSettingsDTO updateDTO) {
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> createDefaultSettings());

        settings.setOpeningTime(updateDTO.getOpeningTime());
        settings.setClosingTime(updateDTO.getClosingTime());
        settings.setOperatingDaysList(updateDTO.getOperatingDays());
        settings.setIsShopOpen(updateDTO.getIsShopOpen());

        ShopSettings savedSettings = shopSettingsRepository.save(settings);
        return convertToDTO(savedSettings);
    }

    /**
     * Add a closed date
     */
    @Transactional
    public ShopSettingsDTO addClosedDate(ClosedDateDTO closedDateDTO) {
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> createDefaultSettings());

        LocalDate date = LocalDate.parse(closedDateDTO.getDate());
        
        // Check if date already exists
        if (closedDateRepository.existsByClosedDate(date)) {
            throw new IllegalArgumentException("Closed date already exists: " + closedDateDTO.getDate());
        }

        ClosedDate closedDate = ClosedDate.builder()
                .closedDate(date)
                .reason(closedDateDTO.getReason())
                .shopSettings(settings)
                .build();

        settings.getClosedDates().add(closedDate);
        closedDateRepository.save(closedDate);

        return convertToDTO(settings);
    }

    /**
     * Remove a closed date
     */
    @Transactional
    public ShopSettingsDTO removeClosedDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        ClosedDate closedDate = closedDateRepository.findByClosedDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("Closed date not found: " + dateStr));

        closedDateRepository.delete(closedDate);

        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> createDefaultSettings());
        
        return convertToDTO(settings);
    }

    /**
     * Check if shop is open on a specific date
     */
    public boolean isShopOpenOnDate(LocalDate date) {
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> createDefaultSettings());

        // Check if shop is globally closed
        if (!settings.getIsShopOpen()) {
            return false;
        }

        // Check if date is in closed dates
        if (closedDateRepository.existsByClosedDate(date)) {
            return false;
        }

        // Check if day of week is in operating days
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Convert to 0-6 (Sun-Sat)
        List<Integer> operatingDays = settings.getOperatingDaysList();
        
        return operatingDays.contains(dayOfWeek);
    }

    /**
     * Create default settings
     */
    private ShopSettings createDefaultSettings() {
        ShopSettings defaultSettings = ShopSettings.builder()
                .openingTime(LocalTime.of(9, 0))  // 9:00 AM
                .closingTime(LocalTime.of(18, 0)) // 6:00 PM
                .operatingDays("1,2,3,4,5")       // Monday to Friday
                .isShopOpen(true)
                .build();

        return shopSettingsRepository.save(defaultSettings);
    }

    /**
     * Convert entity to DTO
     */
    private ShopSettingsDTO convertToDTO(ShopSettings settings) {
        List<String> closedDates = settings.getClosedDates().stream()
                .map(cd -> cd.getClosedDate().toString())
                .sorted()
                .collect(Collectors.toList());

        return ShopSettingsDTO.builder()
                .id(settings.getId())
                .openingTime(settings.getOpeningTime())
                .closingTime(settings.getClosingTime())
                .operatingDays(settings.getOperatingDaysList())
                .isShopOpen(settings.getIsShopOpen())
                .closedDates(closedDates)
                .build();
    }
}
