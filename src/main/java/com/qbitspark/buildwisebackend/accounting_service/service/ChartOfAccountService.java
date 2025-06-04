package com.qbitspark.buildwisebackend.accounting_service.service;

import com.qbitspark.buildwisebackend.accounting_service.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.util.List;
import java.util.UUID;

public interface ChartOfAccountService {
  void createDefaultChartOfAccounts(OrganisationEntity organisation) throws ItemNotFoundException;
  List<ChartOfAccountsResponse> getChartOfAccountsByOrganisationId(UUID organisationId) throws ItemNotFoundException;

  /**
   * Get grouped hierarchical chart of accounts by organisation ID
   * Groups accounts by type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)
   * and maintains parent-child hierarchy within each group
   */
  GroupedChartOfAccountsResponse getGroupedHierarchicalChartOfAccounts(UUID organisationId) throws ItemNotFoundException;


  GroupedChartOfAccountsResponse createDefaultChartOfAccountsAndReturnHierarchical(OrganisationEntity organisation);

}