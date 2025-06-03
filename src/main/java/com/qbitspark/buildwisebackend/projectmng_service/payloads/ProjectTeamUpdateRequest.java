package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeamUpdateRequest {
    @NotNull(message = "Project team is required")
    private Set<AddTeamMemberRequest> teamMembers;
}