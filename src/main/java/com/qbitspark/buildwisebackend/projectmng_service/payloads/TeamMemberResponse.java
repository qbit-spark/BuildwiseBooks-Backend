package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
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
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}