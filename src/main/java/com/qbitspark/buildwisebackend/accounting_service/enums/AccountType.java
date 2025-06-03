package com.qbitspark.buildwisebackend.accounting_service.enums;

public enum AccountType {

    /**
     * Assets - Resources owned by the business that have economic value
     * Account Code Range: 1000-1999
     * Examples: Cash, Accounts Receivable, Inventory, Equipment, Buildings
     * Normal Balance: Debit
     */
    ASSET,

    /**
     * Liabilities - Obligations or debts owed by the business to external parties
     * Account Code Range: 2000-2999
     * Examples: Accounts Payable, Loans Payable, Accrued Expenses, Mortgages
     * Normal Balance: Credit
     */
    LIABILITY,

    /**
     * Equity - Owner's interest in the business (Assets - Liabilities)
     * Account Code Range: 3000-3999
     * Examples: Owner's Capital, Retained Earnings, Common Stock, Additional Paid-in Capital
     * Normal Balance: Credit
     */
    EQUITY,

    /**
     * Revenue - Income earned from business operations
     * Account Code Range: 4000-4999
     * Examples: Sales Revenue, Service Revenue, Interest Income, Rental Income
     * Normal Balance: Credit
     */
    REVENUE,

    /**
     * Expenses - Costs incurred to generate revenue
     * Account Code Range: 5000-5999
     * Examples: Salaries, Rent, Utilities, Advertising, Office Supplies
     * Normal Balance: Debit
     */
    EXPENSE
}