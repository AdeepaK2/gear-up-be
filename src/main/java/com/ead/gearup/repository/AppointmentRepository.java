package com.ead.gearup.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseProjection;
import com.ead.gearup.model.Appointment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.model.Appointment;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByEmployeeEmployeeIdAndDateGreaterThanEqualOrderByDateAsc(Long employeeId, LocalDate date);
    List<Appointment> findByEmployeeEmployeeId(Long employeeId);
    List<Appointment> findByEmployeeEmployeeIdAndDate(Long employeeId, LocalDate date);
    
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.employee.employeeId = :employeeId
            AND YEAR(a.date) = :year
            AND MONTH(a.date) = :month
            AND a.status IN :statuses
            ORDER BY a.date ASC
    """)
    List<Appointment> findAppointmentsByEmployeeAndMonthAndStatus(
        @Param("employeeId") Long employeeId,
        @Param("year") int year,
        @Param("month") int month,
        @Param("statuses") List<AppointmentStatus> statuses
    );

    @Query(value = """
        SELECT DISTINCT a.* FROM appointment a
        JOIN customers c ON a.customer_id = c.customer_id
        JOIN users u ON c.user_id = u.user_id
        LEFT JOIN task t ON t.appointment_id = a.appointment_id
        WHERE a.mechanic_id = :employeeId
          AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """,
    nativeQuery = true
  )
        List<Appointment> searchAppointmentsByCustomerNameOrTask(
            @Param("employeeId") Long employeeId,
            @Param("keyword") String keyword
        );

    List<Appointment> findByEmployeeEmployeeIdAndStatusAndDateAfter(Long employeeId, AppointmentStatus status, LocalDate date);

    @Query(value = "SELECT a.appointment_id AS appointmentId, a.date, a.status, a.notes, a.start_time, a.end_time " +
            "FROM appointment a " +
            "JOIN customers c ON a.customer_id = c.customer_id " +
            "JOIN users u ON c.user_id = u.user_id " +
            "WHERE u.name ILIKE %:name%", nativeQuery = true)
    List<AppointmentSearchResponseProjection> findAppointmentSearchResultsNative(@Param("name") String name);

}
