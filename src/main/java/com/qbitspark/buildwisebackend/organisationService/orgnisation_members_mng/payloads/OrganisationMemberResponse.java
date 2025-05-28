package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrganisationMemberResponse {
    private UUID memberId;
    private String userName;
    private String email;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private String invitedByUserName;
    private boolean isOwner;
    private boolean isAdmin;
    private boolean canManageMembers;
}

