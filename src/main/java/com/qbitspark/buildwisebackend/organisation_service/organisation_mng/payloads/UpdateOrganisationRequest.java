package com.qbitspark.buildwisebackend.organisation_service.organisation_mng.payloads;

import lombok.Data;

@Data
public class UpdateOrganisationRequest {
    private String name;
    private String description;
}
