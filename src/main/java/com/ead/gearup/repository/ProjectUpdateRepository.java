package com.ead.gearup.repository;

import com.ead.gearup.model.ProjectUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, Long> {
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.project.id = :projectId ORDER BY pu.createdAt DESC")
    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.employee.id = :employeeId ORDER BY pu.createdAt DESC")
    List<ProjectUpdate> findByEmployeeIdOrderByCreatedAtDesc(@Param("employeeId") Long employeeId);
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.project.id = :projectId AND pu.updateType = 'COMPLETION' ORDER BY pu.createdAt DESC LIMIT 1")
    ProjectUpdate findLatestCompletionUpdateByProjectId(@Param("projectId") Long projectId);
}
