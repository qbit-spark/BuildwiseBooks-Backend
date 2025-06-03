package com.qbitspark.buildwisebackend.organisation_service.organisation_mng.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganisationRequest {
    @NotBlank(message = "Organisation name is required")
    private String name;
    private String description;
}
