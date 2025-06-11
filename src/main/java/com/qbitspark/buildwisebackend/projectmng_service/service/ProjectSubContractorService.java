package com.qbitspark.buildwisebackend.projectmng_service.service;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.BulkAddSubContractorRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorRemovalResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public interface ProjectSubContractorService {
    List<ProjectSubContractorResponse> addSubContractors(UUID projectId, @Valid Set<BulkAddSubContractorRequest> requests) throws Throwable;

    ProjectSubContractorRemovalResponse removeSubContractors(UUID projectId, Set<UUID> subcontractorIds) throws ItemNotFoundException;

    List<ProjectSubContractorResponse> getProjectSubContractors(UUID projectId) throws ItemNotFoundException;

    boolean isSubContractorAssigned(UUID projectId, UUID subcontractorId) throws ItemNotFoundException;

}
