package com.qbitspark.buildwisebackend.projectmng_service.payloads;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectTeamRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    private String roleName;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

}
