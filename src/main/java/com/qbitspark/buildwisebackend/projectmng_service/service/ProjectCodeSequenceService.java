package com.qbitspark.buildwisebackend.projectmng_service.service;

import java.util.UUID;

public interface ProjectCodeSequenceService {
    public String generateProjectCode(UUID organisationId);
}
