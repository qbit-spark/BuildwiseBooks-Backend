package com.qbitspark.buildwisebackend.accounting_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DefaultChartOfAccountsUtils {

    private static final String SYSTEM_USER = "SYSTEM";

    public static List<ChartOfAccounts> createConstructionChart(OrganisationEntity organisation) {
        List<ChartOfAccounts> accounts = new ArrayList<>();

        accounts.addAll(createAssets(organisation));
        accounts.addAll(createLiabilities(organisation));
        accounts.addAll(createEquity(organisation));
        accounts.addAll(createRevenue(organisation));
        accounts.addAll(createExpenses(organisation));

        return accounts;
    }

    private static List<ChartOfAccounts> createAssets(OrganisationEntity organisation) {
        List<ChartOfAccounts> assets = new ArrayList<>();

        // ASSET HEADERS
        assets.add(createHeaderAccount("1000", "Current Assets", "Short-term assets", AccountType.ASSET, organisation));
        assets.add(createHeaderAccount("1500", "Fixed Assets", "Long-term assets", AccountType.ASSET, organisation));

        // CURRENT ASSET DETAILS - parentAccountId will be set later in service
        assets.add(createDetailAccount("1010", "Cash - Operating", "Main operating cash account", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1020", "Cash - Project Funds", "Project-specific cash reserves", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1100", "Accounts Receivable", "Money owed by customers", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1110", "Retainage Receivable", "Retainage amounts due from customers", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1200", "Work in Progress", "Ongoing construction projects", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1250", "Materials Inventory", "Construction materials on hand", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1300", "Prepaid Expenses", "Insurance and other prepaid items", AccountType.ASSET, organisation));

        // FIXED ASSET DETAILS
        assets.add(createDetailAccount("1510", "Construction Equipment", "Heavy machinery and equipment", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1520", "Trucks & Vehicles", "Company trucks and work vehicles", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1530", "Office Equipment", "Office furniture and equipment", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1600", "Buildings", "Office and warehouse buildings", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1700", "Land", "Real estate land", AccountType.ASSET, organisation));
        assets.add(createDetailAccount("1800", "Accumulated Depreciation", "Depreciation of fixed assets", AccountType.ASSET, organisation));

        return assets;
    }

    private static List<ChartOfAccounts> createLiabilities(OrganisationEntity organisation) {
        List<ChartOfAccounts> liabilities = new ArrayList<>();

        // LIABILITY HEADERS
        liabilities.add(createHeaderAccount("2000", "Current Liabilities", "Short-term obligations", AccountType.LIABILITY, organisation));
        liabilities.add(createHeaderAccount("2500", "Long-term Liabilities", "Long-term obligations", AccountType.LIABILITY, organisation));

        // CURRENT LIABILITY DETAILS
        liabilities.add(createDetailAccount("2010", "Accounts Payable", "Money owed to suppliers", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2020", "Subcontractor Payables", "Amounts owed to subcontractors", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2100", "Retainage Payable", "Retainage held from subcontractors", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2200", "Payroll Liabilities", "Wages and payroll taxes payable", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2300", "Customer Deposits", "Advance payments from customers", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2400", "Short-term Loans", "Short-term loans and notes", AccountType.LIABILITY, organisation));

        // LONG-TERM LIABILITY DETAILS
        liabilities.add(createDetailAccount("2510", "Equipment Loans", "Long-term equipment financing", AccountType.LIABILITY, organisation));
        liabilities.add(createDetailAccount("2600", "Real Estate Mortgages", "Mortgages on real property", AccountType.LIABILITY, organisation));

        return liabilities;
    }

    private static List<ChartOfAccounts> createEquity(OrganisationEntity organisation) {
        List<ChartOfAccounts> equity = new ArrayList<>();

        equity.add(createDetailAccount("3000", "Owner's Capital", "Owner's investment in the business", AccountType.EQUITY, organisation));
        equity.add(createDetailAccount("3100", "Retained Earnings", "Accumulated profits retained in business", AccountType.EQUITY, organisation));
        equity.add(createDetailAccount("3200", "Owner's Draws", "Owner withdrawals from business", AccountType.EQUITY, organisation));

        return equity;
    }

    private static List<ChartOfAccounts> createRevenue(OrganisationEntity organisation) {
        List<ChartOfAccounts> revenue = new ArrayList<>();

        revenue.add(createDetailAccount("4000", "Construction Revenue", "Revenue from construction contracts", AccountType.REVENUE, organisation));
        revenue.add(createDetailAccount("4100", "Change Order Revenue", "Revenue from approved change orders", AccountType.REVENUE, organisation));
        revenue.add(createDetailAccount("4200", "Equipment Rental Income", "Income from renting out equipment", AccountType.REVENUE, organisation));
        revenue.add(createDetailAccount("4900", "Other Income", "Miscellaneous income sources", AccountType.REVENUE, organisation));

        return revenue;
    }

    private static List<ChartOfAccounts> createExpenses(OrganisationEntity organisation) {
        List<ChartOfAccounts> expenses = new ArrayList<>();

        // EXPENSE HEADERS
        expenses.add(createHeaderAccount("5000", "Direct Project Costs", "Costs directly related to projects", AccountType.EXPENSE, organisation));
        expenses.add(createHeaderAccount("5500", "Overhead Expenses", "General business expenses", AccountType.EXPENSE, organisation));

        // DIRECT COST DETAILS
        expenses.add(createDetailAccount("5010", "Materials", "Construction materials", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5100", "Direct Labor", "Direct labor costs", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5200", "Subcontractors", "Subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5300", "Equipment Costs", "Equipment rental and fuel", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5400", "Project Overhead", "Permits, site costs, etc.", AccountType.EXPENSE, organisation));

        // OVERHEAD DETAILS
        expenses.add(createDetailAccount("5510", "Administrative Salaries", "Office and management salaries", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5600", "Office Expenses", "Rent, utilities, supplies", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5700", "Insurance", "General business insurance", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5800", "Professional Services", "Legal, accounting, consulting", AccountType.EXPENSE, organisation));
        expenses.add(createDetailAccount("5900", "Other Expenses", "Marketing, travel, depreciation", AccountType.EXPENSE, organisation));

        return expenses;
    }

    private static ChartOfAccounts createHeaderAccount(String accountCode, String name, String description,
                                                       AccountType accountType, OrganisationEntity organisation) {
        return createAccount(accountCode, name, description, accountType, organisation, true, false);
    }

    private static ChartOfAccounts createDetailAccount(String accountCode, String name, String description,
                                                       AccountType accountType, OrganisationEntity organisation) {
        return createAccount(accountCode, name, description, accountType, organisation, false, true);
    }

    private static ChartOfAccounts createAccount(String accountCode, String name, String description,
                                                 AccountType accountType, OrganisationEntity organisation,
                                                 boolean isHeader, boolean isPostable) {
        ChartOfAccounts account = new ChartOfAccounts();
        account.setAccountCode(accountCode);
        account.setName(name);
        account.setDescription(description);
        account.setAccountType(accountType);
        account.setOrganisation(organisation);
        account.setIsActive(true);
        account.setCreatedDate(LocalDateTime.now());
        account.setCreatedBy(SYSTEM_USER);
        account.setIsHeader(isHeader);
        account.setIsPostable(isPostable);
        account.setParentAccountId(null);
        return account;
    }
}