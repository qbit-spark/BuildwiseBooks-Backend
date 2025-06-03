package com.qbitspark.buildwisebackend.accounting_service.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.accounting_service.utils.DefaultChartOfAccountsUtils;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceIMPL implements ChartOfAccountService {

    private final ChartOfAccountsRepo chartOfAccountsRepository;
    private final OrganisationRepo organisationRepository;

    @Override
    @Transactional
    public List<ChartOfAccounts> createDefaultChartOfAccounts(String organisationId) {
        // Find the organisation
        OrganisationEntity organisation = organisationRepository.findById(UUID.fromString(organisationId))
                .orElseThrow(() -> new RuntimeException("Organisation not found"));

        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);

        return chartOfAccountsRepository.saveAll(defaultAccounts);
    }

//    @Override
//    public List<ChartOfAccounts> getChartOfAccountsByOrganisationId(String organisationId) {
//        return chartOfAccountsRepository.findByOrganisation_OrganisationIdAndIsActiveTrue(
//                UUID.fromString(organisationId)
//        );
//    }
}