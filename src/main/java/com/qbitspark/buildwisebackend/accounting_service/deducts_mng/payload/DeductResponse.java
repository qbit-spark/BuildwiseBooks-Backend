package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductResponse {
    private UUID deductId;
    private String deductName;
    private BigDecimal deductPercent;
    private String deductDescription;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;

    // Organisation info
    private UUID organisationId;
    private String organisationName;
}