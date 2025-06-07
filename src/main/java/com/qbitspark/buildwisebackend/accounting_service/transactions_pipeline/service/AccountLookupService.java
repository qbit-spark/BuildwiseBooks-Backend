package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.service;

import java.util.UUID;

/**
 * Service interface for finding specific accounts in an organisation's chart of accounts.
 *
 * This service abstracts the logic of finding the correct accounts for automatic
 * transaction creation. Each organisation has its own chart of accounts, and this
 * service helps interpreters find the right accounts without knowing the internal
 * structure of the chart of accounts.
 */
public interface AccountLookupService {

    /**
     * Find the main cash account for an organisation.
     * Used for: Cash sales, expense payments, customer payments
     */
    UUID getCashAccountId(UUID organisationId);

    /**
     * Find the accounts receivable account for an organisation.
     * Used for: Customer invoices, credit sales
     */
    UUID getAccountsReceivableAccountId(UUID organisationId);

    /**
     * Find the accounts payable account for an organisation.
     * Used for: Vendor bills, credit purchases, unpaid expenses
     */
    UUID getAccountsPayableAccountId(UUID organisationId);

    /**
     * Find the tax payable account for an organisation.
     * Used for: Sales tax, VAT, other taxes collected
     */
    UUID getTaxPayableAccountId(UUID organisationId);

    /**
     * Find the default revenue account for an organisation.
     * Used for: General sales when no specific revenue account is specified
     */
    UUID getDefaultRevenueAccountId(UUID organisationId);

    /**
     * Find the default expense account for an organisation.
     * Used for: General expenses when no specific expense account is specified
     */
    UUID getDefaultExpenseAccountId(UUID organisationId);

}