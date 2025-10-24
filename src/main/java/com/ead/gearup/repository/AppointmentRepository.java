package com.ead.gearup.repository;

import com.ead.gearup.dto.appointment.AppointmentSearchResponseProjection;
import com.ead.gearup.model.Appointment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query(value = "SELECT a.appointment_id AS appointmentId, a.date, a.status, a.notes, a.start_time, a.end_time " +
            "FROM appointment a " +
            "JOIN customers c ON a.customer_id = c.customer_id " +
            "JOIN users u ON c.user_id = u.user_id " +
            "WHERE u.name ILIKE %:name%", nativeQuery = true)
    List<AppointmentSearchResponseProjection> findAppointmentSearchResultsNative(@Param("name") String name);

}
