package com.ead.gearup.dto.task;

public interface TaskSearchResponseProjection {
    Long getTaskId();

    String getName();

    String getDescription();

    Integer getEstimatedHours();

    Double getCost();

    String getStatus();

    boolean isAssignedProject();

}
