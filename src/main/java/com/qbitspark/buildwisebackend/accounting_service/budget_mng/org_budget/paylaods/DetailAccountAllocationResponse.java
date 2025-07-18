package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.DetailAccountStatus;
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
public class DetailAccountAllocationResponse {
    private UUID detailAccountId;
    private String detailAccountCode;
    private String detailAccountName;
    private String detailDescription;
    private UUID allocationId;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal committedAmount;
    private BigDecimal budgetRemaining;
    private DetailAccountStatus allocationStatus;
    private boolean hasAllocation;
    private String notes;
    private BigDecimal utilizationPercentage;
    private LocalDateTime allocationCreatedDate;
    private LocalDateTime allocationModifiedDate;
}
