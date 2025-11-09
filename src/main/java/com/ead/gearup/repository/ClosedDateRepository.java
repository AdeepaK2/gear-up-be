package com.ead.gearup.repository;

import com.ead.gearup.entity.ClosedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ClosedDateRepository extends JpaRepository<ClosedDate, Long> {
    
    Optional<ClosedDate> findByClosedDate(LocalDate date);
    
    boolean existsByClosedDate(LocalDate date);
}
