package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InvitationInfoResponse {
    private String organisationName;
    private String organisationDescription;
    private String inviterName;
    private String role;
    private String invitedEmail;
    private LocalDateTime invitedAt;
    private LocalDateTime expiresAt;
    private String status;
    private boolean isExpired;
    private boolean canAccept;
    private boolean canDecline;
}