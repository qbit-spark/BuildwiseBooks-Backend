package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTeamMemberRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Role is required")
    private TeamMemberRole role;

    @NotBlank(message = "Contract number is required")
    private String contractNumber;
}