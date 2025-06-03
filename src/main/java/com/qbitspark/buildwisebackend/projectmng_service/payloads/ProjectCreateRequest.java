package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreateRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Project description is required")
    private String description;

    @NotBlank(message = "The contract number is required")
    private String contractNumber;

    @DecimalMax(value = "9999999999999.99", message = "Budget cannot exceed 9,999,999,999,999.99")
    @DecimalMin(value = "0.00", message = "Budget cannot be negative")
    private BigDecimal budget;

    private Set<AddTeamMemberRequest> teamMembers;
}