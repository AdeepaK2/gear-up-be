package com.ead.gearup.repository;

import com.ead.gearup.model.Project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p.status, COUNT(p) " +
           "FROM Project p " +
           "JOIN p.assignedEmployees e " +
           "WHERE e.employeeId = :employeeId " +
           "GROUP BY p.status")
    List<Object[]> countProjectsByStatusForEmployee(@Param("employeeId") Long employeeId);

    List<Project> findByAssignedEmployeesEmployeeId(Long employeeId);

    Optional<Project> findByProjectIdAndAssignedEmployeesEmployeeId(Long projectId, Long employeeId);

    Optional<Project> findByAppointmentAppointmentId(Long appointmentId);

    boolean existsByAppointmentAppointmentId(Long appointmentId);

}
