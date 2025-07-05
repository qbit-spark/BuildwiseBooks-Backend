package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableDetailAccountResponse {

    private UUID detailAccountId;
    private String accountCode;
    private String accountName;
    private String description;
    private boolean isPostable;
    private boolean isActive;

    // Current allocation info (if exists)
    private BigDecimal currentAllocation;
    private BigDecimal remainingAmount;
    private boolean hasExistingAllocation;
}