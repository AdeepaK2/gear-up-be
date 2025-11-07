package com.ead.gearup.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }
}
