package com.qbitspark.buildwisebackend.accounting_service.coa.utils;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountCodeGenerator {

    private final ChartOfAccountsRepo chartOfAccountsRepository;

    /**
     * Generate the next available account code for the given account type and organisation
     */
    public String generateNextAccountCode(AccountType accountType, UUID organisationId, UUID parentAccountId) {

        // Get base range for account type
        int baseCode = getBaseCodeForAccountType(accountType);

        if (parentAccountId != null) {
            // Generate child account code under parent
            return generateChildAccountCode(parentAccountId, organisationId);
        } else {
            // Generate new main account code
            return generateMainAccountCode(baseCode, organisationId);
        }
    }

    /**
     * Get base code range for each account type
     */
    private int getBaseCodeForAccountType(AccountType accountType) {
        return switch (accountType) {
            case ASSET -> 1000;
            case LIABILITY -> 2000;
            case EQUITY -> 3000;
            case REVENUE -> 4000;
            case EXPENSE -> 5000;
        };
    }

    /**
     * Generate main account code (like 1010, 1020, etc.)
     */
    private String generateMainAccountCode(int baseCode, UUID organisationId) {

        // Get all accounts for this organisation in this range
        List<ChartOfAccounts> existingAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        // Find the highest existing code in this range
        int highestCode = baseCode;
        int rangeEnd = baseCode + 999; // e.g., 1000-1999 for assets

        for (ChartOfAccounts account : existingAccounts) {
            try {
                int code = Integer.parseInt(account.getAccountCode());
                if (code >= baseCode && code <= rangeEnd && code > highestCode) {
                    highestCode = code;
                }
            } catch (NumberFormatException e) {
                // Skip non-numeric codes
            }
        }

        // Generate next code in increments of 10
        int nextCode = ((highestCode / 10) + 1) * 10;

        // Make sure we stay within range
        if (nextCode > rangeEnd) {
            nextCode = baseCode + 10; // Start from beginning if we exceed range
        }

        return String.valueOf(nextCode);
    }

    /**
     * Generate child account code under parent (like 1010-01, 1010-02, etc.)
     */
    private String generateChildAccountCode(UUID parentAccountId, UUID organisationId) {

        // Get a parent account
        ChartOfAccounts parent = chartOfAccountsRepository.findById(parentAccountId)
                .orElseThrow(() -> new RuntimeException("Parent account not found"));

        // Get all child accounts under this parent
        List<ChartOfAccounts> allAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        // Find children of this parent
        String parentCode = parent.getAccountCode();
        int highestSubCode = 0;

        for (ChartOfAccounts account : allAccounts) {
            if (parentAccountId.equals(account.getParentAccountId())) {
                String accountCode = account.getAccountCode();

                // Extract sub-code (e.g., from "1010-01" get "01")
                if (accountCode.startsWith(parentCode + "-")) {
                    try {
                        String subCodeStr = accountCode.substring(parentCode.length() + 1);
                        int subCode = Integer.parseInt(subCodeStr);
                        if (subCode > highestSubCode) {
                            highestSubCode = subCode;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid formats
                    }
                }
            }
        }

        // Generate next sub-code
        int nextSubCode = highestSubCode + 1;
        return String.format("%s-%02d", parentCode, nextSubCode);
    }
}
