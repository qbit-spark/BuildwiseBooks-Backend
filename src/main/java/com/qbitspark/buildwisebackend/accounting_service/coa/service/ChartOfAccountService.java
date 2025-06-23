package com.qbitspark.buildwisebackend.accounting_service.coa.service;

import com.qbitspark.buildwisebackend.accounting_service.coa.payload.AddAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.util.UUID;

public interface ChartOfAccountService {

  /**
   * Get grouped hierarchical chart of accounts by organisation ID
   * Throws error if organization doesn't have chart of accounts
   */
  GroupedChartOfAccountsResponse getGroupedHierarchicalChartOfAccounts(UUID organisationId) throws ItemNotFoundException;

  /**
   * Initialize default chart of accounts for organization (one-time setup)
   * Only works if organization doesn't already have chart of accounts
   */
  GroupedChartOfAccountsResponse initializeDefaultChartOfAccounts(UUID organisationId) throws ItemNotFoundException;

  /**
   * Internal method - Create default chart of accounts for new organisation
   * Called automatically during organization creation
   */
  void createDefaultChartOfAccountsAndReturnHierarchical(OrganisationEntity organisation);

  /**
   * Add new account to existing chart of accounts
   * Requires organization to already have chart of accounts
   */
  ChartOfAccountsResponse addNewAccount(AddAccountRequest request) throws ItemNotFoundException;

  /**
   * Update existing account
   */
  ChartOfAccountsResponse updateAccount(UUID accountId, AddAccountRequest request) throws ItemNotFoundException;
}