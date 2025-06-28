package com.qbitspark.buildwisebackend.vendormng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.ProjectResponseForVendor;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.VendorResponse;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.CreateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.UpdateVendorRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface VendorService {

    VendorEntity createVendor(UUID organisationId, CreateVendorRequest request) throws ItemNotFoundException, AccessDeniedException;

    List<VendorEntity> getVendorSummaries(UUID organisationId, VendorType vendorType)
            throws ItemNotFoundException, AccessDeniedException;

    VendorEntity getVendorById(UUID organisationId, UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException;

    Page<VendorEntity> getAllVendors(UUID organisationId, VendorStatus status, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException;

    VendorEntity updateVendor(UUID organisationId, UUID vendorId, UpdateVendorRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    void deleteVendor(UUID organisationId, UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException;
}
