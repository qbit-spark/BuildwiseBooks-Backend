package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubContractorRemovalResponse {
    private int removedCount;
    private Set<UUID> notFoundSubcontractorIds;
    private Set<UUID> notAssignedSubcontractorIds;

    public ProjectSubContractorRemovalResponse(int removedCount) {
        this.removedCount = removedCount;
        this.notFoundSubcontractorIds = Set.of();
        this.notAssignedSubcontractorIds = Set.of();
    }

}
