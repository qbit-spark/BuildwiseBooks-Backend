package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.Data;

import java.util.UUID;

@Data
public class ProjectResponseSummary {
    private String name;
    private UUID projectId;
    private String clientName;
}
