package com.ead.gearup.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProjectStatusDTO {
    String status;
    long count;
    double percentage;
}

