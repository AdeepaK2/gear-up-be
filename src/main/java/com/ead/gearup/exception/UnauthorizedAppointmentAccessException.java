package com.ead.gearup.exception;

public class UnauthorizedAppointmentAccessException extends RuntimeException {
    public UnauthorizedAppointmentAccessException(String message) {
        super(message);
    }
}
