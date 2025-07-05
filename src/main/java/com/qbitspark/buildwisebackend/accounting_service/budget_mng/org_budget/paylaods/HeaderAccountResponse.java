package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public  class HeaderAccountResponse {
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private String description;
    private boolean hasChildren;
    private int childAccountCount;

}
