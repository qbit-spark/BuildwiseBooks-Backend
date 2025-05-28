package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserOrganisationResponse {
    private UUID organisationId;
    private String organisationName;
    private String organisationDescription;
    private String myRole;
    private String myStatus;
    private LocalDateTime joinedAt;
    private boolean isOwner;
    private boolean isAdmin;
    private boolean canManageMembers;
    private boolean canInviteMembers;
    private int totalMembers;
    private int totalPendingInvitations;
    private String ownerUserName;
}