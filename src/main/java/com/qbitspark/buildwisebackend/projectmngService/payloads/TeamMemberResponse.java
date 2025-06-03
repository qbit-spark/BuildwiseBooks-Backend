package com.qbitspark.buildwisebackend.projectmngService.payloads;

import com.qbitspark.buildwisebackend.projectmngService.enums.TeamMemberRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {

    private UUID memberId;
    private String memberName;
    private String email;
    private TeamMemberRole role;
    private String roleDisplayName;
    private String contractNumber;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;

    public TeamMemberResponse(UUID memberId, String memberName, String email,
                              TeamMemberRole role, String contractNumber, String status,
                              LocalDateTime joinedAt, LocalDateTime updatedAt) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.email = email;
        this.role = role;
        this.roleDisplayName = role != null ? role.getDisplayName() : null;
        this.contractNumber = contractNumber;
        this.status = status;
        this.joinedAt = joinedAt;
        this.updatedAt = updatedAt;
    }

    public void setRole(TeamMemberRole role) {
        this.role = role;
        this.roleDisplayName = role != null ? role.getDisplayName() : null;
    }
}