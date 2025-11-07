package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ead.gearup.model.TimeLog;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
}
