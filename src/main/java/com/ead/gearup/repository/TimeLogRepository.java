package com.ead.gearup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ead.gearup.model.TimeLog;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    
    @Query("SELECT COALESCE(SUM(tl.hoursWorked), 0.0) FROM TimeLog tl WHERE tl.project.projectId = :projectId")
    Double getTotalLoggedHoursByProjectId(@Param("projectId") Long projectId);
}
