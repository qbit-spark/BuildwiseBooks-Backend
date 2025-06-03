package com.qbitspark.buildwisebackend.projectmng_service.payloads;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeamUpdateRequest {
    @NotNull(message = "Team member IDs are required")
    private Set<UUID> teamMemberIds;

}
