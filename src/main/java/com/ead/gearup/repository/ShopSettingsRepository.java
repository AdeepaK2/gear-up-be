package com.ead.gearup.repository;

import com.ead.gearup.entity.ShopSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopSettingsRepository extends JpaRepository<ShopSettings, Long> {
    
    // There should only be one settings record
    Optional<ShopSettings> findFirstByOrderByIdAsc();
}
