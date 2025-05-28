package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads;

import lombok.Data;

import java.util.List;

@Data
public class OrganisationMembersOverviewResponse {
    private String organisationName;
    private int totalMembers;
    private int totalPendingInvitations;
    private int totalActiveMembers;
    private int totalSuspendedMembers;
    private List<OrganisationMemberResponse> members;
    private List<PendingInvitationResponse> pendingInvitations;
    private List<PendingInvitationResponse> declinedInvitations;
}
