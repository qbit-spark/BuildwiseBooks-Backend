package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import java.util.UUID;

@Data
public class BulkAddTeamMemberRequest {

    @NotEmpty(message = "At least one member ID is required")
    private Set<UUID> memberIds;

    @NotNull(message = "Role is required")
    private TeamMemberRole role;

}
