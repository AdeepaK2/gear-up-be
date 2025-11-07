package com.ead.gearup.enums;

import lombok.Getter;

@Getter
public enum ConsultationType {
    GENERAL_CHECKUP("General Checkup"),
    SPECIFIC_ISSUE("Specific Issue"),
    MAINTENANCE_ADVICE("Maintenance Advice"),
    PERFORMANCE_ISSUE("Performance Issue"),
    SAFETY_CONCERN("Safety Concern"),
    OTHER("Other");

    private final String label;

    ConsultationType(String label) {
        this.label = label;
    }
}
