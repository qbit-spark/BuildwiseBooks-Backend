package com.qbitspark.buildwisebackend.subcontractor_service.payloads;

import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectResponse;
import com.qbitspark.buildwisebackend.subcontractor_service.enums.SpecializationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubcontractorResponse {

    private UUID subcontractorId;
    private String companyName;
    private String email;
    private String phoneNumber;
    private String tin;
    private String address;
    private String registrationNumber;
    private List<SpecializationType> specializations;
    private String organisationName;
    private UUID organisationId;
    private List<ProjectResponseForSubcontractor> projects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int projectsCount;
    private int specializationsCount;
}
