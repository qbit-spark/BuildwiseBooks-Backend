package com.qbitspark.buildwisebackend.organisationService.payloads;

import lombok.Data;

@Data
public class UpdateOrganisationRequest {
    private String name;
    private String description;
}
