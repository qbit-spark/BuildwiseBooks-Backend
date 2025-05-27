package com.qbitspark.buildwisebackend.organisationService.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganisationRequest {
    @NotBlank(message = "Organisation name is required")
    private String name;
    private String description;
}
