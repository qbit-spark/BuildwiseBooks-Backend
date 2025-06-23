package com.qbitspark.buildwisebackend.accounting_service.tax_mng.service;


import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.CreateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.UpdateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.TaxResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface TaxService {

    /**
     * Create new tax for organisation
     */
    TaxResponse createTax(UUID organisationId, CreateTaxRequest request) throws ItemNotFoundException;

    /**
     * Get all taxes for organisation
     */
    List<TaxResponse> getAllTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException;

    /**
     * Get active taxes for organisation
     */
    List<TaxResponse> getActiveTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException;

    /**
     * Get tax by ID
     */
    TaxResponse getTaxById(UUID organisationId, UUID taxId) throws ItemNotFoundException;

    /**
     * Update existing tax
     */
    TaxResponse updateTax(UUID organisationId, UUID taxId, UpdateTaxRequest request) throws ItemNotFoundException;

    /**
     * Delete tax (hard delete - permanently remove)
     */
    void deleteTax(UUID organisationId, UUID taxId) throws ItemNotFoundException;
}