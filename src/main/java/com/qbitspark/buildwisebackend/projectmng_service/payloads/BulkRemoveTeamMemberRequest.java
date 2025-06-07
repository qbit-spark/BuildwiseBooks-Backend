package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class BulkRemoveTeamMemberRequest {

    @NotEmpty(message = "At least one member ID is required")
    private Set<UUID> memberIds;

}