package com.qbitspark.buildwisebackend.projectmng_service.controller;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.BulkAddSubContractorRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorRemovalResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorResponse;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectSubContractorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/subcontractors")
@RequiredArgsConstructor
@Slf4j
public class ProjectSubContractorController {

    private final ProjectSubContractorService projectSubContractorService;

    @PostMapping()
    public ResponseEntity<GlobeSuccessResponseBuilder> addSubContractors(
            @PathVariable UUID projectId,
            @Valid @RequestBody Set<BulkAddSubContractorRequest> requests) throws Exception {

        List<ProjectSubContractorResponse> responses = null;
        try {
            responses = projectSubContractorService.addSubContractors(projectId, requests);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(String.format("Successfully added %d subcontractors", responses.size()), responses));
    }

    @DeleteMapping()
    public ResponseEntity<GlobeSuccessResponseBuilder> removeSubContractors(
            @PathVariable UUID projectId,
            @RequestBody Set<UUID> subcontractorIds) throws ItemNotFoundException {

        ProjectSubContractorRemovalResponse responses = projectSubContractorService.removeSubContractors(projectId, subcontractorIds);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(String.format("Successfully removed %d subcontractors", responses.getRemovedCount()), responses)
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectSubContractors(
            @PathVariable UUID projectId) throws ItemNotFoundException {

        List<ProjectSubContractorResponse> responses = projectSubContractorService.getProjectSubContractors(projectId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Retrieved %d subcontractors", responses.size()),
                        responses
                )
        );
    }

    @GetMapping("/check")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkSubContractorAssignment(
            @PathVariable UUID projectId, @RequestParam UUID subcontractorId) throws ItemNotFoundException {

        boolean isSubContractorAssigned = projectSubContractorService.isSubContractorAssigned(projectId, subcontractorId);

        SubContractorAssignmentCheck result = new SubContractorAssignmentCheck(subcontractorId, projectId, isSubContractorAssigned);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(String.format("Subcontractor %s %s assigned to project", subcontractorId, isSubContractorAssigned ? "is" : "is not"),
                        result)
        );
    }

    // Simple response class for assignment check
    public static class SubContractorAssignmentCheck {
        public UUID subcontractorId;
        public UUID projectId;
        public boolean isSubContractorAssigned;

        public SubContractorAssignmentCheck(UUID subcontractorId, UUID projectId, boolean isSubContractorAssigned) {
            this.subcontractorId = subcontractorId;
            this.projectId = projectId;
            this.isSubContractorAssigned = isSubContractorAssigned;
        }
    }
}