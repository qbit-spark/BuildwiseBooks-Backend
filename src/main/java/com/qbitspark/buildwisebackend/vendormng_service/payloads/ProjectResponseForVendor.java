package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectResponseForVendor {
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