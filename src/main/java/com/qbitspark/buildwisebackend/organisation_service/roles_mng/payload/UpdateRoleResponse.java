package com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UpdateRoleResponse {
    private String id;
    private String name;
    private String description;
    private Map<String, Map<String, Boolean>> permissions;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Boolean isDefaultRole;
}