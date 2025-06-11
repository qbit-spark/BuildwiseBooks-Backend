package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddSubContractorRequest {
    @NotNull
    private UUID subcontractorId;
}