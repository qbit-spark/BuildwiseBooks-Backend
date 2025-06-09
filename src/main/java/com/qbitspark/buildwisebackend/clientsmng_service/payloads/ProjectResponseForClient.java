package com.qbitspark.buildwisebackend.clientsmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.payloads.TeamMemberResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class ProjectResponseForClient {
    private UUID projectId;
    private String name;
    private String description;
    private BigDecimal budget;
    private String organisationName;
    private UUID organisationId;
    private String status;
    private String contractNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
