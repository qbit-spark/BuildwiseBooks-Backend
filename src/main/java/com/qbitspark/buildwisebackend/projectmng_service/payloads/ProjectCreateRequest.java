package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreateRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Project description is required")
    private String description;

    @NotBlank(message = "The contract number is required")
    private String contractNumber;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

}