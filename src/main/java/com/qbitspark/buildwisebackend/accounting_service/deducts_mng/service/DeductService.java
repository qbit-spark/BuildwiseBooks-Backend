package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service;

import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.CreateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.UpdateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.DeductResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface DeductService {

    DeductResponse createDeduct(UUID organisationId, CreateDeductRequest request) throws ItemNotFoundException, AccessDeniedException;

    List<DeductResponse> getAllDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    List<DeductResponse> getActiveDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    DeductResponse getDeductById(UUID organisationId, UUID deductId) throws ItemNotFoundException, AccessDeniedException;

    DeductResponse updateDeduct(UUID organisationId, UUID deductId, UpdateDeductRequest request) throws ItemNotFoundException, AccessDeniedException;

    void deleteDeduct(UUID organisationId, UUID deductId) throws ItemNotFoundException, AccessDeniedException;
}