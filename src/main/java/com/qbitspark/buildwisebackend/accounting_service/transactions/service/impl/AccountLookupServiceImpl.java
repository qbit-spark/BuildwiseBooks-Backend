package com.qbitspark.buildwisebackend.accounting_service.transactions.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.transactions.service.AccountLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLookupServiceImpl implements AccountLookupService {

    private final ChartOfAccountsRepo chartOfAccountsRepo;

    @Override
    public UUID getCashAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.ASSET,
                new String[]{"1010", "1020"}, // Account codes for cash accounts
                new String[]{"cash", "operating", "project funds"} // Keywords in account names
        );
    }

    @Override
    public UUID getAccountsReceivableAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.ASSET,
                new String[]{"1100"}, // Standard A/R account code
                new String[]{"receivable", "accounts receivable", "a/r"}
        );
    }

    @Override
    public UUID getAccountsPayableAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.LIABILITY,
                new String[]{"2010"}, // Standard A/P account code
                new String[]{"payable", "accounts payable", "a/p"}
        );
    }

    @Override
    public UUID getTaxPayableAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.LIABILITY,
                new String[]{}, // No standard code - varies by country
                new String[]{"tax", "vat", "sales tax", "tax payable"}
        );
    }

    @Override
    public UUID getDefaultRevenueAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.REVENUE,
                new String[]{"4000"}, // Construction revenue
                new String[]{"construction revenue", "revenue", "sales"}
        );
    }

    @Override
    public UUID getDefaultExpenseAccountId(UUID organisationId) {
        return findAccountByCodeOrName(organisationId, AccountType.EXPENSE,
                new String[]{"5000", "5010"}, // Direct project costs
                new String[]{"materials", "direct costs", "expense"}
        );
    }

    /**
     * Core method that searches for accounts by code patterns or name keywords
     */
    private UUID findAccountByCodeOrName(UUID organisationId, AccountType accountType,
                                         String[] accountCodes, String[] nameKeywords) {

        List<ChartOfAccounts> accounts = chartOfAccountsRepo.findByOrganisation_OrganisationId(organisationId);

        // Strategy 1: Try to find by exact account code match
        for (String code : accountCodes) {
            Optional<UUID> accountId = accounts.stream()
                    .filter(account -> account.getAccountType() == accountType)
                    .filter(account -> account.getIsActive() && account.getIsPostable())
                    .filter(account -> code.equals(account.getAccountCode()))
                    .map(ChartOfAccounts::getId)
                    .findFirst();

            if (accountId.isPresent()) {
                log.debug("Found account by code '{}' for organisation {}", code, organisationId);
                return accountId.get();
            }
        }

        // Strategy 2: Try to find by account code prefix
        for (String codePrefix : accountCodes) {
            Optional<UUID> accountId = accounts.stream()
                    .filter(account -> account.getAccountType() == accountType)
                    .filter(account -> account.getIsActive() && account.getIsPostable())
                    .filter(account -> account.getAccountCode().startsWith(codePrefix))
                    .map(ChartOfAccounts::getId)
                    .findFirst();

            if (accountId.isPresent()) {
                log.debug("Found account by code prefix '{}' for organisation {}", codePrefix, organisationId);
                return accountId.get();
            }
        }

        // Strategy 3: Try to find by name keywords
        for (String keyword : nameKeywords) {
            Optional<UUID> accountId = accounts.stream()
                    .filter(account -> account.getAccountType() == accountType)
                    .filter(account -> account.getIsActive() && account.getIsPostable())
                    .filter(account -> account.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .map(ChartOfAccounts::getId)
                    .findFirst();

            if (accountId.isPresent()) {
                log.debug("Found account by keyword '{}' for organisation {}", keyword, organisationId);
                return accountId.get();
            }
        }

        // Strategy 4: Fallback - return first account of the correct type
        Optional<UUID> fallbackAccount = accounts.stream()
                .filter(account -> account.getAccountType() == accountType)
                .filter(account -> account.getIsActive() && account.getIsPostable())
                .map(ChartOfAccounts::getId)
                .findFirst();

        if (fallbackAccount.isPresent()) {
            log.warn("Using fallback account of type {} for organisation {}", accountType, organisationId);
            return fallbackAccount.get();
        }

        // No suitable account found
        log.error("No suitable {} account found for organisation {}", accountType, organisationId);
        return null;
    }
}