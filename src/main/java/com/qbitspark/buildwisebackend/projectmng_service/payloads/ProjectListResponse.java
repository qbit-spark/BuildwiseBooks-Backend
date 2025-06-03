package com.qbitspark.buildwisebackend.projectmng_service.payloads;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListResponse {

    private UUID projectId;
    private String projectName;
    private String projectDescription;
    private BigDecimal budget;
    private String status;
    private String organisationName;
    private int teamMembersCount;
    private LocalDateTime createdAt;
    private String contractNumber;

}
