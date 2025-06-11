package com.qbitspark.buildwisebackend.projectmng_service.payloads;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubContractorResponse {
    private UUID id;
    private UUID projectId;
    private UUID subcontractorId;
    private String companyName;
    private LocalDateTime assignmentDate;
}
