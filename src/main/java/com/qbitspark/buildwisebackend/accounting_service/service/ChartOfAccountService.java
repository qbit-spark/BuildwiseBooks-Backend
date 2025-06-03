package com.qbitspark.buildwisebackend.accounting_service.service;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;

import java.util.List;

public interface ChartOfAccountService {
  List<ChartOfAccounts> createDefaultChartOfAccounts(String organisationId);
  //List<ChartOfAccounts> getChartOfAccountsByOrganisationId(String organisationId);
}
