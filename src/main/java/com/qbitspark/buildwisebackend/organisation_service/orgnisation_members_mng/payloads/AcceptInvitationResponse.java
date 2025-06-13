package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads;

import lombok.Data;

import java.util.UUID;

@Data
public class AcceptInvitationResponse {
    private String organisationName;
    private UUID organisationId;

}
