package com.ead.gearup.repository;

import com.ead.gearup.model.Project;
import com.ead.gearup.enums.ProjectStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByStatus(ProjectStatus status);

    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countProjectsByStatus();


    @Query(value = """
            SELECT DATE_TRUNC('month', p.updated_at) AS month_start, COUNT(*)
            FROM projects p
            WHERE p.status = :status AND p.updated_at >= :startDate
            GROUP BY month_start
            ORDER BY month_start
            """, nativeQuery = true)
    List<Object[]> countProjectsByMonthAndStatus(@Param("status") String status, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT p.status, COUNT(p) " +
           "FROM Project p " +
           "JOIN p.assignedEmployees e " +
           "WHERE e.employeeId = :employeeId " +
           "GROUP BY p.status")
    List<Object[]> countProjectsByStatusForEmployee(@Param("employeeId") Long employeeId);

    List<Project> findByAssignedEmployeesEmployeeId(Long employeeId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH p.vehicle " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.mainRepresentativeEmployee m " +
           "LEFT JOIN FETCH m.user " +
           "WHERE EXISTS (SELECT 1 FROM p.assignedEmployees e2 WHERE e2.employeeId = :employeeId) " +
           "   OR (p.mainRepresentativeEmployee IS NOT NULL AND p.mainRepresentativeEmployee.employeeId = :employeeId)")
    List<Project> findByAssignedEmployeesEmployeeIdOrMainRepresentativeEmployeeIdWithDetails(@Param("employeeId") Long employeeId);

    Optional<Project> findByProjectIdAndAssignedEmployeesEmployeeId(Long projectId, Long employeeId);

    Optional<Project> findByAppointmentAppointmentId(Long appointmentId);

    boolean existsByAppointmentAppointmentId(Long appointmentId);

    @Query("SELECT p FROM Project p " +
           "LEFT JOIN FETCH p.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH p.vehicle " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.mainRepresentativeEmployee mre " +
           "LEFT JOIN FETCH mre.user " +
           "WHERE c.customerId = :customerId")
    List<Project> findAllByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH p.vehicle " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.mainRepresentativeEmployee mre " +
           "LEFT JOIN FETCH mre.user")
    List<Project> findAllWithDetails();
    
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.assignedEmployees ae " +
           "LEFT JOIN FETCH ae.user " +
           "WHERE p.projectId IN :projectIds")
    List<Project> fetchAssignedEmployees(@Param("projectIds") List<Long> projectIds);
    
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.tasks " +
           "WHERE p.projectId IN :projectIds")
    List<Project> fetchTasks(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH p.vehicle " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.mainRepresentativeEmployee mre " +
           "LEFT JOIN FETCH mre.user " +
           "WHERE p.projectId = :projectId")
    Optional<Project> findByIdWithDetails(@Param("projectId") Long projectId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.customer c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH p.vehicle " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.mainRepresentativeEmployee mre " +
           "LEFT JOIN FETCH mre.user " +
           "WHERE c.customerId = :customerId AND p.reportSentToCustomer = true AND p.status = :status")
    List<Project> findProjectsWithReportsByCustomerId(@Param("customerId") Long customerId, @Param("status") ProjectStatus status);

}
