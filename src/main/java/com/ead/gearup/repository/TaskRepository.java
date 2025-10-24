package com.ead.gearup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.dto.task.TaskSearchResponseProjection;
import com.ead.gearup.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(value = "SELECT t.task_id AS taskId, t.name AS name, t.description AS description, " +
            "t.estimated_hours AS estimatedHours, t.cost AS cost, t.status AS status, " +
            "t.is_assigned_project AS assignedProject, t.appointment_id AS appointmentId " +
            "FROM task t " +
            "WHERE t.name ILIKE %:name%", nativeQuery = true)
    List<TaskSearchResponseProjection> findTaskSearchResultsNative(@Param("name") String name);

}
