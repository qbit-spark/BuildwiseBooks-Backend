package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTeamMemberRoleRequest {

    @NotNull(message = "New role is required")
    private TeamMemberRole newRole;

}
