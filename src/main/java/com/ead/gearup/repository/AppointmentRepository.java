package com.ead.gearup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ead.gearup.dto.appointment.AppointmentSearchResponseProjection;
import com.ead.gearup.model.Appointment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCustomer(Customer customer);
}
