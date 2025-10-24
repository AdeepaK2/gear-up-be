package com.ead.gearup.enums;

import lombok.Getter;

@Getter
public enum ConsultationType {
    ENGINE_CHECK("Engine Check"),
    OIL_CHANGE("Oil Change"),
    BRAKE_INSPECTION("Brake Inspection"),
    TIRE_ROTATION("Tire Rotation"),
    FULL_SERVICE("Full Vehicle Service"),
    BATTERY_TEST("Battery Test"),
    DIAGNOSTIC_SCAN("Diagnostic Scan"),
    OTHER("Other");

    private final String label;

    ConsultationType(String label) {
        this.label = label;
    }
}
