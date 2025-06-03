package com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.payloads;

import lombok.Data;
import java.util.List;

@Data
public class UserOrganisationsOverviewResponse {
    private String userName;
    private int totalOrganisations;
    private int ownedOrganisations;
    private int memberOrganisations;
    private List<UserOrganisationResponse> organisations;
}
