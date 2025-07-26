package com.qbitspark.buildwisebackend.accounting_service.tax_mng.service;


import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.CreateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.UpdateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.TaxResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface TaxService {


    TaxResponse createTax(UUID organisationId, CreateTaxRequest request) throws ItemNotFoundException, AccessDeniedException;

    List<TaxResponse> getAllTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    List<TaxResponse> getActiveTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    TaxResponse getTaxById(UUID organisationId, UUID taxId) throws ItemNotFoundException, AccessDeniedException;

    TaxResponse updateTax(UUID organisationId, UUID taxId, UpdateTaxRequest request) throws ItemNotFoundException, AccessDeniedException;


    void deleteTax(UUID organisationId, UUID taxId) throws ItemNotFoundException, AccessDeniedException;
}