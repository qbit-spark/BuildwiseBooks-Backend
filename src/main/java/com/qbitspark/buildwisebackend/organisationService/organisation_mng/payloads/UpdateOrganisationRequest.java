package com.qbitspark.buildwisebackend.organisationservice.organisation_mng.payloads;

import lombok.Data;

@Data
public class UpdateOrganisationRequest {
    private String name;
    private String description;
}
