package com.ead.gearup.dto.project;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ProjectAdditionalRequestDTO {

    @NotBlank(message = "Custom request cannot be empty")
    private String customRequest;

    @NotNull(message = "Reference file is required")
    private MultipartFile referenceFile;
}
