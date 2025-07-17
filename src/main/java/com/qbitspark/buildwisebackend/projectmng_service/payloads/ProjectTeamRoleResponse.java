package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectTeamRoleResponse {
    private UUID roleId;
    private String roleName;
    private String description;
    private Boolean isDefaultRole;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String createdBy;
}