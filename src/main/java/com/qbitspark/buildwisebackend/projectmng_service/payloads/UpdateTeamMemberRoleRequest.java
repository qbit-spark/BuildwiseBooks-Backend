package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTeamMemberRoleRequest {

    @NotNull(message = "New role ID is required")
    private UUID newRoleId;

}
