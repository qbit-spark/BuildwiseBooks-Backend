package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;


@Getter
public enum DetailAccountStatus {
    UNALLOCATED("No budget allocation"),
    AWAITING_FUNDING("Budget allocated but not funded"),
    ACTIVE("Funded and available for spending"),
    PARTIALLY_SPENT("Some amount spent, balance remaining"),
    FULLY_SPENT("All funded amount spent"),
    OVERSPENT("Spent more than funded amount"),
    SUSPENDED("Account suspended for spending");

    private final String description;

    DetailAccountStatus(String description) {
        this.description = description;
    }

}