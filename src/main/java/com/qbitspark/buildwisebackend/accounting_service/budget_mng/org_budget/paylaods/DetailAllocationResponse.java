package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailAllocationResponse {

    private UUID allocationId;

    // Header account info
    private UUID headerLineItemId;
    private String headerAccountCode;
    private String headerAccountName;

    // Detail account info
    private UUID detailAccountId;
    private String detailAccountCode;
    private String detailAccountName;

    // Allocation amounts
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal committedAmount;
    private BigDecimal remainingAmount;

    // Metadata
    private String allocationNotes;
    private boolean hasAllocation;
    private String allocationStatus;
    private BigDecimal utilizationPercentage;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
