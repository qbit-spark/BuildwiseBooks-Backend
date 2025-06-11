package com.qbitspark.buildwisebackend.subcontractor_service.payloads;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProjectResponseForSubcontractor {
    private UUID projectId;
    private String projectCode;
    private String name;
    private String description;
    private BigDecimal budget;
    private String organisationName;
    private UUID organisationId;
    private List<ProjectResponseForSubcontractor> projects;
    private String status;
    private String contractNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
