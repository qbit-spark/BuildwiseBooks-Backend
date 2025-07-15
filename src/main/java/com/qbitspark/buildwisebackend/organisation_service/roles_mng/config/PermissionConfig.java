package com.qbitspark.buildwisebackend.organisation_service.roles_mng.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionConfig {

    public static final String ORGANISATION = "ORGANISATION";
    public static final String PROJECTS = "PROJECTS";
    public static final String INVOICES = "INVOICES";
    public static final String VOUCHERS = "VOUCHERS";
    public static final String CLIENTS = "CLIENTS";
    public static final String VENDORS = "VENDORS";
    public static final String BUDGET = "BUDGET";
    public static final String TRANSACTIONS = "TRANSACTIONS";
    public static final String RECEIPTS = "RECEIPTS";
    public static final String CHART_OF_ACCOUNTS = "CHART_OF_ACCOUNTS";
    public static final String DRIVE = "DRIVE";
    public static final String SYSTEM = "SYSTEM";
    public static final String BANK_ACCOUNTS = "BANK_ACCOUNTS";
    public static final String DEDUCTS = "DEDUCTS";
    private static final String TAXES = "TAXES";

    // Get all available resources
    public static List<String> getAllResources() {
        return Arrays.asList(
                ORGANISATION, PROJECTS, INVOICES, VOUCHERS, CLIENTS, VENDORS,
                BUDGET, TRANSACTIONS, RECEIPTS, CHART_OF_ACCOUNTS, DRIVE, SYSTEM, BANK_ACCOUNTS, DEDUCTS, TAXES
        );
    }

    // Get default permissions for a resource (all false)
    public static Map<String, Boolean> getResourcePermissions(String resource) {
        return switch (resource) {
            case ORGANISATION -> getOrganisationPermissions();
            case PROJECTS -> getProjectPermissions();
            case INVOICES -> getInvoicePermissions();
            case VOUCHERS -> getVoucherPermissions();
            case CLIENTS -> getClientPermissions();
            case VENDORS -> getVendorPermissions();
            case BUDGET -> getBudgetPermissions();
            case TRANSACTIONS -> getTransactionsPermissions();
            case RECEIPTS -> getReceiptPermissions();
            case CHART_OF_ACCOUNTS -> getChartOfAccountsPermissions();
            case DRIVE -> getDrivePermissions();
            case SYSTEM -> getSystemPermissions();
            case BANK_ACCOUNTS -> getBankAccountsPermissions();
            case DEDUCTS -> getDeductsPermissions();
            case TAXES -> getTaxesPermissions();
            default -> new HashMap<>();
        };
    }

    // Get all permissions for all resources (for creating default roles)
    public static Map<String, Map<String, Boolean>> getAllPermissions() {
        Map<String, Map<String, Boolean>> allPermissions = new HashMap<>();
        getAllResources().forEach(resource ->
                allPermissions.put(resource, getResourcePermissions(resource))
        );
        return allPermissions;
    }

    // Individual permission methods
    private static Map<String, Boolean> getOrganisationPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createOrganisation", false);
        permissions.put("updateOrganisation", false);
        permissions.put("viewOrganisation", false);
        permissions.put("deleteOrganisation", false);
        permissions.put("manageMembers", false);
        permissions.put("inviteMembers", false);
        permissions.put("removeMembers", false);
        permissions.put("manageInvitations", false);
        permissions.put("viewMembers", false);
        return permissions;
    }

    private static Map<String, Boolean> getProjectPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createProject", false);
        permissions.put("updateProject", false);
        permissions.put("deleteProject", false);
        permissions.put("viewProjects", false);
        permissions.put("manageTeam", false);
        permissions.put("updateTeamRoles", false);
        permissions.put("viewTeamMembers", false);
        return permissions;
    }

    private static Map<String, Boolean> getInvoicePermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createInvoice", false);
        permissions.put("updateInvoice", false);
        permissions.put("deleteInvoice", false);
        permissions.put("viewInvoices", false);
        permissions.put("sendInvoice", false);
        permissions.put("manageInvoiceAttachments", false);
        return permissions;
    }

    private static Map<String, Boolean> getVoucherPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createVoucher", false);
        permissions.put("updateVoucher", false);
        permissions.put("deleteVoucher", false);
        permissions.put("viewVouchers", false);
        permissions.put("approveVoucher", false);
        permissions.put("processVoucher", false);
        permissions.put("manageVoucherAttachments", false);
        return permissions;
    }

    private static Map<String, Boolean> getClientPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createClient", false);
        permissions.put("updateClient", false);
        permissions.put("deleteClient", false);
        permissions.put("viewClients", false);
        permissions.put("viewClientProjects", false);
        return permissions;
    }

    private static Map<String, Boolean> getVendorPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createVendor", false);
        permissions.put("updateVendor", false);
        permissions.put("deleteVendor", false);
        permissions.put("viewVendors", false);
        permissions.put("manageBankDetails", false);
        return permissions;
    }

    private static Map<String, Boolean> getBudgetPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createBudget", false);
        permissions.put("updateBudget", false);
        permissions.put("deleteBudget", false);
        permissions.put("viewBudget", false);
        permissions.put("activateBudget", false);
        permissions.put("distributeBudget", false);
        permissions.put("allocateBudget", false);
        permissions.put("viewBudgetSummary", false);
        permissions.put("viewBudgetAccounts", false);
        return permissions;
    }

    private static Map<String, Boolean> getTransactionsPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("viewFinancialReports", false);
        permissions.put("exportReports", false);
        return permissions;
    }

    private static Map<String, Boolean> getReceiptPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createReceipt", false);
        permissions.put("updateReceipt", false);
        permissions.put("deleteReceipt", false);
        permissions.put("viewReceipts", false);
        permissions.put("cancelReceipt", false);
        permissions.put("viewPaymentHistory", false);
        permissions.put("createAllocation", false);
        permissions.put("viewAllocations", false);
        return permissions;
    }

    private static Map<String, Boolean> getChartOfAccountsPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("viewChartOfAccounts", false);
        permissions.put("createAccounts", false);
        permissions.put("updateAccounts", false);
        permissions.put("deleteAccounts", false);
        permissions.put("manageAccountHierarchy", false);
        return permissions;
    }

    private static Map<String, Boolean> getDrivePermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("uploadFiles", false);
        permissions.put("viewFiles", false);
        permissions.put("deleteFiles", false);
        permissions.put("manageProjectFiles", false);
        permissions.put("downloadFiles", false);
        return permissions;
    }

    private static Map<String, Boolean> getSystemPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("manageUserAccounts", false);
        permissions.put("viewSystemLogs", false);
        permissions.put("manageSystemSettings", false);
        permissions.put("accessAdminPanel", false);
        return permissions;
    }

    private static Map<String, Boolean> getBankAccountsPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createBankAccounts", false);
        permissions.put("updateBankAccounts", false);
        permissions.put("setDefaultBankAccounts", false);
        permissions.put("deactivateBankAccounts", false);
        permissions.put("viewBankAccounts", false);
        return permissions;
    }

    private static Map<String, Boolean> getDeductsPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createDeducts", false);
        permissions.put("updateDeducts", false);
        permissions.put("deleteDeducts", false);
        permissions.put("viewDeducts", false);
        permissions.put("manageDeducts", false);
        return permissions;
    }

    private static Map<String, Boolean> getTaxesPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("createTaxes", false);
        permissions.put("updateTaxes", false);
        permissions.put("viewTaxes", false);
        permissions.put("manageTaxes", false);
        permissions.put("deleteTaxes", false);
        return permissions;
    }


}