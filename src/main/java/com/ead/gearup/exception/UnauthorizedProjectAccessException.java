package com.ead.gearup.exception;

public class UnauthorizedProjectAccessException extends RuntimeException {
    public UnauthorizedProjectAccessException(String message) {
        super(message);
    }
}
