package com.qbitspark.buildwisebackend.subcontractor_service.payloads;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class SubcontractorAssignRequest {

    @NotNull
    private List<UUID> subcontractorIds;

    public List<UUID> getSubcontractorIds() {
        return subcontractorIds;
    }

    public void setSubcontractorIds(List<UUID> subcontractorIds) {
        this.subcontractorIds = subcontractorIds;
    }
}