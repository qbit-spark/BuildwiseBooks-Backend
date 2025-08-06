package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserPendingInvitationResponse {
    private UUID invitationId;
    private String token;
    private UUID organisationId;
    private String organisationName;
    private String organisationDescription;
    private String inviterName;
    private String inviterEmail;
    private String role;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime expiresAt;
    private boolean isExpired;
    private boolean canAccept;
    private boolean canDecline;
}