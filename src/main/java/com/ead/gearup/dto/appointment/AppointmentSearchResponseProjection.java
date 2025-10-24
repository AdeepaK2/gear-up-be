package com.ead.gearup.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentSearchResponseProjection {
    Long getAppointmentId();

    LocalDate getDate();

    String getStatus();

    String getNotes();

    LocalTime getStartTime();

    LocalTime getEndTime();
}
