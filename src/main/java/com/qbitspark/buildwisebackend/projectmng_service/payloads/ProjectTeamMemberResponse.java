package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectTeamMemberResponse {
    private UUID memberId;
    private String memberName;
    private String memberEmail;
    private UUID roleId;
    private String roleName;
    private String roleDescription;
    private String organisationRole;
    private String status;
    private LocalDateTime joinedAt;
    private String addedBy;
}