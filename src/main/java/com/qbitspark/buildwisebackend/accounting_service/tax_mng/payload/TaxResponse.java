package com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxResponse {
    private UUID taxId;
    private String taxName;
    private Double taxPercent;
    private String taxDescription;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;

    // Organisation info
    private UUID organisationId;
    private String organisationName;
}