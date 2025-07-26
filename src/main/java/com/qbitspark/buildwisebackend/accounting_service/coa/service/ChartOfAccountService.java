package com.qbitspark.buildwisebackend.accounting_service.coa.service;

import com.qbitspark.buildwisebackend.accounting_service.coa.payload.AddAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.util.UUID;

public interface ChartOfAccountService {

  GroupedChartOfAccountsResponse getGroupedHierarchicalChartOfAccounts(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;


  GroupedChartOfAccountsResponse initializeDefaultChartOfAccounts(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;


  void createDefaultChartOfAccountsAndReturnHierarchical(OrganisationEntity organisation);


  ChartOfAccountsResponse addNewAccount(UUID organisationId, AddAccountRequest request) throws ItemNotFoundException, AccessDeniedException;

  ChartOfAccountsResponse updateAccount(UUID organisationId, UUID accountId, AddAccountRequest request) throws ItemNotFoundException, AccessDeniedException;
}