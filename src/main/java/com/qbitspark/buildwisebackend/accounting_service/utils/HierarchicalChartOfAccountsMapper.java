// File: HierarchicalChartOfAccountsMapper.java
package com.qbitspark.buildwisebackend.accounting_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.payload.HierarchicalAccountResponse;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HierarchicalChartOfAccountsMapper {

    /**
     * Convert flat list of accounts to grouped hierarchical structure
     */
    public GroupedChartOfAccountsResponse toGroupedHierarchicalResponse(List<ChartOfAccounts> allAccounts) {
        if (allAccounts == null || allAccounts.isEmpty()) {
            return GroupedChartOfAccountsResponse.builder()
                    .accountsByType(new LinkedHashMap<>())
                    .build();
        }

        // Get organisation info from first account
        OrganisationEntity organisation = allAccounts.get(0).getOrganisation();

        // Build grouped hierarchical structure
        Map<AccountType, List<HierarchicalAccountResponse>> groupedAccounts =
                buildGroupedHierarchicalStructure(allAccounts);

        return GroupedChartOfAccountsResponse.builder()
                .organisationId(organisation.getOrganisationId())
                .organisationName(organisation.getOrganisationName())
                .accountsByType(groupedAccounts)
                .build();
    }

    /**
     * Build grouped hierarchical structure from flat list
     */
    private Map<AccountType, List<HierarchicalAccountResponse>> buildGroupedHierarchicalStructure(
            List<ChartOfAccounts> allAccounts) {

        Map<AccountType, List<HierarchicalAccountResponse>> result = new LinkedHashMap<>();

        // Group accounts by AccountType first
        Map<AccountType, List<ChartOfAccounts>> accountsByType = allAccounts.stream()
                .collect(Collectors.groupingBy(ChartOfAccounts::getAccountType));

        // Process each account type in a specific order (Assets, Liabilities, Equity, Revenue, Expenses)
        List<AccountType> orderedTypes = Arrays.asList(
                AccountType.ASSET,
                AccountType.LIABILITY,
                AccountType.EQUITY,
                AccountType.REVENUE,
                AccountType.EXPENSE
        );

        for (AccountType accountType : orderedTypes) {
            List<ChartOfAccounts> accountsOfType = accountsByType.get(accountType);

            if (accountsOfType != null && !accountsOfType.isEmpty()) {
                List<HierarchicalAccountResponse> hierarchicalAccounts =
                        buildHierarchyForAccountType(accountsOfType);
                result.put(accountType, hierarchicalAccounts);
            }
        }

        return result;
    }

    /**
     * Build hierarchy within a specific account type
     */
    private List<HierarchicalAccountResponse> buildHierarchyForAccountType(
            List<ChartOfAccounts> accountsOfType) {

        // Convert to response objects
        Map<UUID, HierarchicalAccountResponse> accountMap = accountsOfType.stream()
                .collect(Collectors.toMap(
                        ChartOfAccounts::getId,
                        this::convertToHierarchicalAccountResponse
                ));

        // Build parent-child relationships within this account type
        List<HierarchicalAccountResponse> rootAccounts = new ArrayList<>();

        for (HierarchicalAccountResponse account : accountMap.values()) {
            if (account.getParentAccountId() == null) {
                // Root account within this type
                rootAccounts.add(account);
            } else {
                // Child account - add to parent's children list
                HierarchicalAccountResponse parent = accountMap.get(account.getParentAccountId());
                if (parent != null) {
                    parent.getChildren().add(account);
                } else {
                    // Parent not found in same type - treat as root
                    rootAccounts.add(account);
                }
            }
        }

        // Sort by account code recursively
        sortAccountsRecursively(rootAccounts);

        return rootAccounts;
    }

    /**
     * Convert entity to hierarchical response
     */
    private HierarchicalAccountResponse convertToHierarchicalAccountResponse(ChartOfAccounts entity) {
        return HierarchicalAccountResponse.builder()
                .id(entity.getId())
                .accountCode(entity.getAccountCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate())
                .createdBy(entity.getCreatedBy())
                .parentAccountId(entity.getParentAccountId())
                .isHeader(entity.getIsHeader())
                .isPostable(entity.getIsPostable())
                .children(new ArrayList<>())
                .build();
    }

    /**
     * Sort accounts recursively by account code
     */
    private void sortAccountsRecursively(List<HierarchicalAccountResponse> accounts) {
        accounts.sort(Comparator.comparing(HierarchicalAccountResponse::getAccountCode));
        accounts.forEach(account -> sortAccountsRecursively(account.getChildren()));
    }
}