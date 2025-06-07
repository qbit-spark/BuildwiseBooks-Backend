package com.qbitspark.buildwisebackend.projectmngService.payloads;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {
    private UUID projectId;
    private String name;
    private String description;
    private BigDecimal budget;
    private String organisationName;
    private UUID organisationId;
    private String status;
    private String contractNumber;
    private Set<TeamMemberResponse> teamMembers;
    private TeamMemberResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID clientId;
    private String clientName;
}
