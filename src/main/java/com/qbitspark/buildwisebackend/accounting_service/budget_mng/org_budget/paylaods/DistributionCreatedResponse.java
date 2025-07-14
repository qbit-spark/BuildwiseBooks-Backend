package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class DistributionCreatedResponse {
    private UUID distributionId;
    private String detailAccountCode;
    private String detailAccountName;
    private BigDecimal distributedAmount;
    private String description;
}
