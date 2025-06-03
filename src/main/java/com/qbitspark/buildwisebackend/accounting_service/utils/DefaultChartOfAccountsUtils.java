package com.qbitspark.buildwisebackend.accounting_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating construction industry specific chart of accounts
 * Includes all accounts typically needed for construction companies
 */
public class DefaultChartOfAccountsUtils {

    private static final String SYSTEM_USER = "SYSTEM";

    /**
     * Creates a comprehensive construction company chart of accounts
     * @param organisation The organisation entity
     * @return List of construction-specific chart of accounts
     */
    public static List<ChartOfAccounts> createConstructionChart(OrganisationEntity organisation) {
        List<ChartOfAccounts> accounts = new ArrayList<>();

        accounts.addAll(createConstructionAssets(organisation));
        accounts.addAll(createConstructionLiabilities(organisation));
        accounts.addAll(createConstructionEquity(organisation));
        accounts.addAll(createConstructionRevenue(organisation));
        accounts.addAll(createConstructionExpenses(organisation));

        return accounts;
    }

    /**
     * Creates construction-specific asset accounts (1000-1999)
     */
    private static List<ChartOfAccounts> createConstructionAssets(OrganisationEntity organisation) {
        List<ChartOfAccounts> assets = new ArrayList<>();

        // CURRENT ASSETS (1000-1499)
        // Cash & Cash Equivalents (1000-1099)
        assets.add(createAccount("1000", "Cash - Operating", "Main operating cash account", AccountType.ASSET, organisation));
        assets.add(createAccount("1010", "Cash - Payroll", "Dedicated payroll cash account", AccountType.ASSET, organisation));
        assets.add(createAccount("1020", "Cash - Project Funds", "Project-specific cash reserves", AccountType.ASSET, organisation));
        assets.add(createAccount("1030", "Petty Cash", "Small cash fund for minor expenses", AccountType.ASSET, organisation));
        assets.add(createAccount("1040", "Cash - Escrow", "Funds held in escrow", AccountType.ASSET, organisation));

        // Accounts Receivable (1100-1199)
        assets.add(createAccount("1100", "Accounts Receivable - Trade", "Standard customer receivables", AccountType.ASSET, organisation));
        assets.add(createAccount("1110", "Accounts Receivable - Retainage", "Retainage amounts due from customers", AccountType.ASSET, organisation));
        assets.add(createAccount("1120", "Accounts Receivable - Progress Billing", "Billed but unpaid progress payments", AccountType.ASSET, organisation));
        assets.add(createAccount("1130", "Accounts Receivable - Change Orders", "Receivables from approved change orders", AccountType.ASSET, organisation));
        assets.add(createAccount("1140", "Contract Assets - Unbilled", "Earned revenue not yet billed", AccountType.ASSET, organisation));
        assets.add(createAccount("1150", "Allowance for Doubtful Accounts", "Estimated uncollectible receivables", AccountType.ASSET, organisation));

        // Inventory & Materials (1200-1299)
        assets.add(createAccount("1200", "Raw Materials Inventory", "Construction materials on hand", AccountType.ASSET, organisation));
        assets.add(createAccount("1210", "Work in Progress - Materials", "Materials allocated to active projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1220", "Work in Progress - Labor", "Labor costs allocated to active projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1230", "Work in Progress - Equipment", "Equipment costs allocated to active projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1240", "Work in Progress - Subcontractors", "Subcontractor costs on active projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1250", "Work in Progress - Overhead", "Overhead allocated to active projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1260", "Small Tools & Supplies", "Hand tools and consumable supplies", AccountType.ASSET, organisation));

        // Other Current Assets (1300-1499)
        assets.add(createAccount("1300", "Prepaid Insurance", "Insurance premiums paid in advance", AccountType.ASSET, organisation));
        assets.add(createAccount("1310", "Prepaid Licenses & Permits", "Permits and licenses paid in advance", AccountType.ASSET, organisation));
        assets.add(createAccount("1320", "Prepaid Equipment Rentals", "Equipment rental payments in advance", AccountType.ASSET, organisation));
        assets.add(createAccount("1330", "Security Deposits", "Refundable deposits paid", AccountType.ASSET, organisation));
        assets.add(createAccount("1340", "Employee Advances", "Cash advances to employees", AccountType.ASSET, organisation));
        assets.add(createAccount("1350", "Job Mobilization Costs", "Costs to mobilize for new projects", AccountType.ASSET, organisation));
        assets.add(createAccount("1400", "Short-term Investments", "Temporary cash investments", AccountType.ASSET, organisation));

        // FIXED ASSETS (1500-1999)
        // Construction Equipment (1500-1699)
        assets.add(createAccount("1500", "Heavy Equipment", "Bulldozers, excavators, cranes", AccountType.ASSET, organisation));
        assets.add(createAccount("1510", "Accumulated Depreciation - Heavy Equipment", "Depreciation of heavy equipment", AccountType.ASSET, organisation));
        assets.add(createAccount("1520", "Trucks & Vehicles", "Company trucks and work vehicles", AccountType.ASSET, organisation));
        assets.add(createAccount("1530", "Accumulated Depreciation - Vehicles", "Depreciation of vehicles", AccountType.ASSET, organisation));
        assets.add(createAccount("1540", "Tools & Equipment", "Power tools and equipment", AccountType.ASSET, organisation));
        assets.add(createAccount("1550", "Accumulated Depreciation - Tools", "Depreciation of tools", AccountType.ASSET, organisation));
        assets.add(createAccount("1560", "Scaffolding & Formwork", "Reusable construction forms", AccountType.ASSET, organisation));
        assets.add(createAccount("1570", "Accumulated Depreciation - Scaffolding", "Depreciation of scaffolding", AccountType.ASSET, organisation));

        // Buildings & Improvements (1700-1899)
        assets.add(createAccount("1700", "Office Buildings", "Office and administrative buildings", AccountType.ASSET, organisation));
        assets.add(createAccount("1710", "Accumulated Depreciation - Buildings", "Depreciation of buildings", AccountType.ASSET, organisation));
        assets.add(createAccount("1720", "Warehouse & Storage", "Material storage facilities", AccountType.ASSET, organisation));
        assets.add(createAccount("1730", "Accumulated Depreciation - Warehouse", "Depreciation of warehouse", AccountType.ASSET, organisation));
        assets.add(createAccount("1740", "Shop & Maintenance Facility", "Equipment maintenance facilities", AccountType.ASSET, organisation));
        assets.add(createAccount("1750", "Accumulated Depreciation - Shop", "Depreciation of shop facilities", AccountType.ASSET, organisation));
        assets.add(createAccount("1800", "Furniture & Office Equipment", "Office furniture and equipment", AccountType.ASSET, organisation));
        assets.add(createAccount("1810", "Accumulated Depreciation - Furniture", "Depreciation of furniture", AccountType.ASSET, organisation));

        // Land & Other Assets (1900-1999)
        assets.add(createAccount("1900", "Land", "Real estate land (non-depreciable)", AccountType.ASSET, organisation));
        assets.add(createAccount("1910", "Equipment Under Capital Lease", "Leased equipment treated as owned", AccountType.ASSET, organisation));
        assets.add(createAccount("1920", "Accumulated Depreciation - Leased Equipment", "Depreciation of leased equipment", AccountType.ASSET, organisation));
        assets.add(createAccount("1950", "Long-term Investments", "Long-term investment securities", AccountType.ASSET, organisation));

        return assets;
    }

    /**
     * Creates construction-specific liability accounts (2000-2999)
     */
    private static List<ChartOfAccounts> createConstructionLiabilities(OrganisationEntity organisation) {
        List<ChartOfAccounts> liabilities = new ArrayList<>();

        // CURRENT LIABILITIES (2000-2499)
        // Accounts Payable (2000-2099)
        liabilities.add(createAccount("2000", "Accounts Payable - Trade", "Standard supplier payables", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2010", "Accounts Payable - Subcontractors", "Amounts owed to subcontractors", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2020", "Accounts Payable - Material Suppliers", "Materials supplier payables", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2030", "Accounts Payable - Equipment Rental", "Equipment rental payables", AccountType.LIABILITY, organisation));

        // Retainage & Progress Billing (2100-2199)
        liabilities.add(createAccount("2100", "Retainage Payable - Subcontractors", "Retainage held from subcontractors", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2110", "Contract Liabilities - Unearned", "Advance payments from customers", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2120", "Progress Billing in Excess of Costs", "Overbilled amounts on projects", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2130", "Customer Deposits", "Job deposits and down payments", AccountType.LIABILITY, organisation));

        // Payroll & Benefits (2200-2299)
        liabilities.add(createAccount("2200", "Wages Payable", "Unpaid employee wages", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2210", "Payroll Taxes Payable", "Employer payroll tax obligations", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2220", "Workers' Compensation Payable", "Workers' comp insurance payable", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2230", "Health Insurance Payable", "Employee health insurance payable", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2240", "Retirement Plan Payable", "401k and pension obligations", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2250", "Union Dues Payable", "Union dues withheld from employees", AccountType.LIABILITY, organisation));

        // Taxes & Government (2300-2399)
        liabilities.add(createAccount("2300", "Income Tax Payable", "Corporate income taxes owed", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2310", "Sales Tax Payable", "Sales tax collected but not remitted", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2320", "Property Tax Payable", "Property taxes on equipment and facilities", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2330", "Unemployment Tax Payable", "State and federal unemployment taxes", AccountType.LIABILITY, organisation));

        // Other Current Liabilities (2400-2499)
        liabilities.add(createAccount("2400", "Short-term Notes Payable", "Short-term loans and notes", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2410", "Equipment Loans - Current Portion", "Current portion of equipment loans", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2420", "Accrued Interest Payable", "Interest accrued on loans", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2430", "Performance Bond Payable", "Performance bond obligations", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2440", "Warranty Reserves", "Reserves for warranty work", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2450", "Equipment Rental Deposits", "Security deposits for rented equipment", AccountType.LIABILITY, organisation));

        // LONG-TERM LIABILITIES (2500-2999)
        // Long-term Debt (2500-2699)
        liabilities.add(createAccount("2500", "Long-term Notes Payable", "Long-term loans and financing", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2510", "Equipment Loans Payable", "Long-term equipment financing", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2520", "Real Estate Mortgages", "Mortgages on real property", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2530", "SBA Loans Payable", "Small Business Administration loans", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2540", "Capital Lease Obligations", "Long-term capital lease payments", AccountType.LIABILITY, organisation));

        // Other Long-term Liabilities (2700-2999)
        liabilities.add(createAccount("2700", "Deferred Tax Liability", "Future tax obligations", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2710", "Pension Obligations", "Long-term pension liabilities", AccountType.LIABILITY, organisation));
        liabilities.add(createAccount("2720", "Post-retirement Benefits", "Healthcare and other retirement benefits", AccountType.LIABILITY, organisation));

        return liabilities;
    }

    /**
     * Creates construction-specific equity accounts (3000-3999)
     */
    private static List<ChartOfAccounts> createConstructionEquity(OrganisationEntity organisation) {
        List<ChartOfAccounts> equity = new ArrayList<>();

        equity.add(createAccount("3000", "Owner's Capital", "Owner's investment in the business", AccountType.EQUITY, organisation));
        equity.add(createAccount("3100", "Retained Earnings", "Accumulated profits retained in business", AccountType.EQUITY, organisation));
        equity.add(createAccount("3200", "Common Stock", "Common shares issued", AccountType.EQUITY, organisation));
        equity.add(createAccount("3300", "Additional Paid-in Capital", "Amount paid above par value of stock", AccountType.EQUITY, organisation));
        equity.add(createAccount("3400", "Owner's Draws", "Owner withdrawals from business", AccountType.EQUITY, organisation));
        equity.add(createAccount("3500", "Treasury Stock", "Company's own stock repurchased", AccountType.EQUITY, organisation));
        equity.add(createAccount("3600", "Accumulated Other Comprehensive Income", "Other comprehensive income items", AccountType.EQUITY, organisation));

        return equity;
    }

    /**
     * Creates construction-specific revenue accounts (4000-4999)
     */
    private static List<ChartOfAccounts> createConstructionRevenue(OrganisationEntity organisation) {
        List<ChartOfAccounts> revenue = new ArrayList<>();

        // Contract Revenue (4000-4299)
        revenue.add(createAccount("4000", "Contract Revenue - Residential", "Revenue from residential construction", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4010", "Contract Revenue - Commercial", "Revenue from commercial construction", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4020", "Contract Revenue - Industrial", "Revenue from industrial projects", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4030", "Contract Revenue - Government", "Revenue from government contracts", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4040", "Contract Revenue - Maintenance", "Revenue from maintenance contracts", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4050", "Contract Revenue - Remodeling", "Revenue from remodeling work", AccountType.REVENUE, organisation));

        // Change Orders & Additional Work (4300-4399)
        revenue.add(createAccount("4300", "Change Order Revenue", "Revenue from approved change orders", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4310", "Extra Work Revenue", "Revenue from additional work orders", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4320", "Cost Plus Revenue", "Revenue from cost-plus contracts", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4330", "Time & Material Revenue", "Revenue from T&M contracts", AccountType.REVENUE, organisation));

        // Other Revenue (4400-4999)
        revenue.add(createAccount("4400", "Equipment Rental Income", "Income from renting out equipment", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4410", "Material Sales", "Revenue from selling excess materials", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4500", "Interest Income", "Income from investments and bank deposits", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4600", "Gain on Sale of Equipment", "Profit from equipment sales", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4700", "Insurance Claims Recovery", "Recovery from insurance claims", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4800", "Warranty Work Revenue", "Revenue from warranty work", AccountType.REVENUE, organisation));
        revenue.add(createAccount("4900", "Other Income", "Miscellaneous income sources", AccountType.REVENUE, organisation));

        return revenue;
    }

    /**
     * Creates construction-specific expense accounts (5000-5999)
     */
    private static List<ChartOfAccounts> createConstructionExpenses(OrganisationEntity organisation) {
        List<ChartOfAccounts> expenses = new ArrayList<>();

        // DIRECT PROJECT COSTS (5000-5499)
        // Materials (5000-5099)
        expenses.add(createAccount("5000", "Materials - Concrete", "Concrete and related materials", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5010", "Materials - Steel & Rebar", "Steel, rebar, and metal materials", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5020", "Materials - Lumber", "Lumber and wood products", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5030", "Materials - Electrical", "Electrical supplies and fixtures", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5040", "Materials - Plumbing", "Plumbing supplies and fixtures", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5050", "Materials - HVAC", "Heating, ventilation, and A/C materials", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5060", "Materials - Roofing", "Roofing materials and supplies", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5070", "Materials - Masonry", "Brick, block, and masonry supplies", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5080", "Materials - Insulation", "Insulation materials", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5090", "Materials - Other", "Other construction materials", AccountType.EXPENSE, organisation));

        // Labor (5100-5199)
        expenses.add(createAccount("5100", "Direct Labor - Carpenters", "Carpenter wages and benefits", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5110", "Direct Labor - Electricians", "Electrician wages and benefits", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5120", "Direct Labor - Plumbers", "Plumber wages and benefits", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5130", "Direct Labor - Masons", "Mason wages and benefits", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5140", "Direct Labor - General Labor", "General laborer wages", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5150", "Direct Labor - Equipment Operators", "Heavy equipment operator wages", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5160", "Payroll Taxes - Direct Labor", "Payroll taxes on direct labor", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5170", "Workers' Compensation - Direct", "Workers' comp on direct labor", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5180", "Employee Benefits - Direct", "Benefits for direct labor employees", AccountType.EXPENSE, organisation));

        // Subcontractors (5200-5299)
        expenses.add(createAccount("5200", "Subcontractors - Excavation", "Excavation and site work", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5210", "Subcontractors - Concrete", "Concrete subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5220", "Subcontractors - Framing", "Framing subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5230", "Subcontractors - Electrical", "Electrical subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5240", "Subcontractors - Plumbing", "Plumbing subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5250", "Subcontractors - HVAC", "HVAC subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5260", "Subcontractors - Roofing", "Roofing subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5270", "Subcontractors - Flooring", "Flooring subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5280", "Subcontractors - Painting", "Painting subcontractor costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5290", "Subcontractors - Other", "Other subcontractor costs", AccountType.EXPENSE, organisation));

        // Equipment & Tools (5300-5399)
        expenses.add(createAccount("5300", "Equipment Rental", "Rental of construction equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5310", "Equipment Fuel", "Fuel for company equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5320", "Equipment Maintenance", "Maintenance and repairs of equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5330", "Small Tools", "Hand tools and small equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5340", "Safety Equipment", "Safety gear and equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5350", "Equipment Parts", "Parts for equipment repairs", AccountType.EXPENSE, organisation));

        // Project Overhead (5400-5499)
        expenses.add(createAccount("5400", "Permits & Licenses", "Building permits and licenses", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5410", "Site Utilities", "Temporary utilities for job sites", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5420", "Site Security", "Security services for job sites", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5430", "Job Site Facilities", "Temporary facilities and trailers", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5440", "Project Insurance", "Job-specific insurance costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5450", "Cleanup & Disposal", "Job site cleanup and waste disposal", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5460", "Testing & Inspection", "Third-party testing and inspection", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5470", "Mobilization", "Costs to mobilize for projects", AccountType.EXPENSE, organisation));

        // INDIRECT COSTS / OVERHEAD (5500-5999)
        // Administrative Salaries (5500-5599)
        expenses.add(createAccount("5500", "Administrative Salaries", "Office and management salaries", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5510", "Project Management Salaries", "Project manager compensation", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5520", "Estimating Department", "Estimator salaries and costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5530", "Accounting Department", "Accounting staff salaries", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5540", "Sales & Marketing Salaries", "Sales staff compensation", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5550", "Payroll Taxes - Administrative", "Payroll taxes on admin staff", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5560", "Employee Benefits - Administrative", "Benefits for admin employees", AccountType.EXPENSE, organisation));

        // Office & Administrative (5600-5699)
        expenses.add(createAccount("5600", "Office Rent", "Office facility rental costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5610", "Office Utilities", "Office electricity, water, etc.", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5620", "Office Supplies", "General office supplies", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5630", "Telephone & Internet", "Phone and internet services", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5640", "Computer & Software", "Computer equipment and software", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5650", "Office Equipment Lease", "Copiers, printers, etc.", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5660", "Postage & Shipping", "Mail and shipping costs", AccountType.EXPENSE, organisation));

        // Insurance & Bonding (5700-5799)
        expenses.add(createAccount("5700", "General Liability Insurance", "General business liability", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5710", "Auto Insurance", "Vehicle insurance premiums", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5720", "Equipment Insurance", "Insurance on equipment", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5730", "Property Insurance", "Building and property insurance", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5740", "Performance Bonds", "Performance bond premiums", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5750", "Payment Bonds", "Payment bond premiums", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5760", "Umbrella Insurance", "Umbrella liability coverage", AccountType.EXPENSE, organisation));

        // Professional Services (5800-5899)
        expenses.add(createAccount("5800", "Legal Fees", "Attorney and legal costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5810", "Accounting Fees", "CPA and bookkeeping services", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5820", "Consulting Fees", "Business consulting services", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5830", "Engineering Fees", "Engineering and design services", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5840", "Architectural Fees", "Architectural services", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5850", "Survey Fees", "Land surveying services", AccountType.EXPENSE, organisation));

        // Marketing & Business Development (5900-5999)
        expenses.add(createAccount("5900", "Advertising", "Print, radio, TV advertising", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5910", "Marketing Materials", "Brochures, business cards, etc.", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5920", "Trade Shows", "Trade show participation costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5930", "Business Entertainment", "Client entertainment expenses", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5940", "Travel Expenses", "Business travel costs", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5950", "Vehicle Expenses", "Vehicle fuel and maintenance", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5960", "Memberships & Subscriptions", "Professional memberships", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5970", "Training & Education", "Employee training and education", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5980", "Depreciation Expense", "Depreciation of fixed assets", AccountType.EXPENSE, organisation));
        expenses.add(createAccount("5990", "Bad Debt Expense", "Estimated uncollectible receivables", AccountType.EXPENSE, organisation));

        return expenses;
    }

    /**
     * Helper method to create a chart of accounts entity
     */
    private static ChartOfAccounts createAccount(String accountCode, String name, String description,
                                                 AccountType accountType, OrganisationEntity organisation) {
        ChartOfAccounts account = new ChartOfAccounts();
        account.setAccountCode(accountCode);
        account.setName(name);
        account.setDescription(description);
        account.setAccountType(accountType);
        account.setOrganisation(organisation);
        account.setActive(true);
        account.setCreatedDate(LocalDateTime.now());
        account.setCreatedBy(SYSTEM_USER);
        return account;
    }
}