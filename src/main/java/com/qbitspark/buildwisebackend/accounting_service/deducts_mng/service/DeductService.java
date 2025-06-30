package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service;

import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.CreateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.UpdateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.DeductResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface DeductService {

    /**
     * Create new deduct for organisation
     */
    DeductResponse createDeduct(UUID organisationId, CreateDeductRequest request) throws ItemNotFoundException;

    /**
     * Get all deducts for organisation
     */
    List<DeductResponse> getAllDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException;

    /**
     * Get active deducts for organisation
     */
    List<DeductResponse> getActiveDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException;

    /**
     * Get deduct by ID
     */
    DeductResponse getDeductById(UUID organisationId, UUID deductId) throws ItemNotFoundException;

    /**
     * Update existing deduct
     */
    DeductResponse updateDeduct(UUID organisationId, UUID deductId, UpdateDeductRequest request) throws ItemNotFoundException;

    /**
     * Delete deduct (hard delete - permanently remove)
     */
    void deleteDeduct(UUID organisationId, UUID deductId) throws ItemNotFoundException;
}