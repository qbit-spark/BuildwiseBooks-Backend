package com.qbitspark.buildwisebackend.accounting_service.payload;

import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedChartOfAccountsResponse {
    private UUID organisationId;
    private String organisationName;
    private Map<AccountType, List<HierarchicalAccountResponse>> accountsByType;
}
