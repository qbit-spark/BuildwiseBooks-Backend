package com.qbitspark.buildwisebackend.projectmng_service.payloads;
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
    private String projectCode;
    private String name;
    private String description;
    private String organisationName;
    private UUID organisationId;
    private String status;
    private BigDecimal contractSum;
    private String contractNumber;
    private Set<TeamMemberResponse> teamMembers;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID clientId;
    private String clientName;
}
