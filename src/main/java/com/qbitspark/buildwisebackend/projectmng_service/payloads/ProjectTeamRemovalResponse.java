package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProjectTeamRemovalResponse {
    private List<ProjectTeamMemberResponse> removedMembers;
    private List<UUID> skippedMemberIds;
    private List<UUID> protectedOwnerIds;
    private int totalRequested;
    private int totalRemoved;
    private int totalSkipped;
    private int totalProtected;
    private String message;
}
