package com.qbitspark.buildwisebackend.vendormng_service.service;

import com.qbitspark.buildwisebackend.vendormng_service.payloads.ProjectResponseForVendor;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.VendorResponse;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.CreateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.UpdateVendorRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface VendorService {

    VendorResponse createVendorWithinOrganisation(UUID organisationId, CreateVendorRequest request) throws ItemNotFoundException;

    VendorResponse getVendorByIdWithinOrganisation(UUID vendorId) throws ItemNotFoundException;

    List<VendorResponse> getAllVendorsWithinOrganisation(UUID organisationId) throws ItemNotFoundException;

    VendorResponse updateVendorWithinOrganisation(UUID vendorId, UpdateVendorRequest request) throws ItemNotFoundException;

    void deleteVendorWithinOrganisation(UUID vendorId) throws ItemNotFoundException;

   // List<ProjectResponseForVendor> getVendorProjectsWithinOrganisation(UUID vendorId) throws ItemNotFoundException;
}