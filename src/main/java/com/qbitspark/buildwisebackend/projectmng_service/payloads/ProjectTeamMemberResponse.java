package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import lombok.Data;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectTeamMemberResponse {
    private UUID memberId;
    private String memberName;
    private String memberEmail;
    private TeamMemberRole role;
    private String organisationRole; // OWNER, ADMIN, MEMBER
    private String status; // ACTIVE, SUSPENDED, etc.
    private LocalDateTime joinedAt;
}
