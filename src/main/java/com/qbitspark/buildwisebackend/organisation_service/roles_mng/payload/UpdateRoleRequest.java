package com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateRoleRequest {

    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    private String name;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @NotNull(message = "Permissions are required")
    @NotEmpty(message = "At least one permission must be provided")
    private Map<String, Map<String, Boolean>> permissions;
}