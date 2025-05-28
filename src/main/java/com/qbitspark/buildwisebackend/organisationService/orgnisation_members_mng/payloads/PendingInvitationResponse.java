package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PendingInvitationResponse {
    private UUID invitationId;
    private String email;
    private String role;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime expiresAt;
    private String invitedByUserName;
    private boolean isExpired;
    private boolean canResend;
    private boolean canRevoke;
}
