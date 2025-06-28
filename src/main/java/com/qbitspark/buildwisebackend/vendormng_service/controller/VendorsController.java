package com.qbitspark.buildwisebackend.vendormng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.*;
import com.qbitspark.buildwisebackend.vendormng_service.service.VendorService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/organisation/{organisationId}/vendors")
@RequiredArgsConstructor
public class VendorsController {

    private final VendorService vendorService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createVendor(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateVendorRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        VendorEntity vendorEntity = vendorService.createVendor(organisationId, request);
        VendorResponse response = mapToVendorResponse(vendorEntity);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendor created successfully",
                        response
                )
        );
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getVendor(
            @PathVariable UUID organisationId,
            @PathVariable UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException {

        VendorEntity vendorEntity = vendorService.getVendorById(organisationId, vendorId);
        VendorResponse response = mapToVendorResponse(vendorEntity);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendor retrieved successfully",
                        response
                )
        );
    }


    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllVendors(
            @PathVariable UUID organisationId,
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection)
            throws ItemNotFoundException, AccessDeniedException {


        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);


        Page<VendorEntity> vendorPage = vendorService.getAllVendors(organisationId, status, pageable);


        Page<VendorResponse> responsePage = vendorPage.map(this::mapToVendorResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendors retrieved successfully",
                        responsePage
                )
        );
    }

    @GetMapping("/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getVendorSummaries(
            @PathVariable UUID organisationId,
            @RequestParam(required = false) VendorType vendorType)
            throws ItemNotFoundException, AccessDeniedException {

        List<VendorEntity> vendorEntities = vendorService.getVendorSummaries(organisationId, vendorType);
        List<VendorSummaryResponse> responses = vendorEntities.stream()
                .map(this::mapToVendorSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendor summaries retrieved successfully",
                        responses
                )
        );
    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateVendor(
            @PathVariable UUID organisationId,
            @PathVariable UUID vendorId,
            @Valid @RequestBody UpdateVendorRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        VendorEntity vendorEntity = vendorService.updateVendor(organisationId, vendorId, request);
        VendorResponse response = mapToVendorResponse(vendorEntity);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendor updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteVendor(
            @PathVariable UUID organisationId,
            @PathVariable UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException {

        vendorService.deleteVendor(organisationId, vendorId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Vendor deleted successfully"
                )
        );
    }


    private VendorResponse mapToVendorResponse(VendorEntity vendor) {
        VendorResponse response = new VendorResponse();
        response.setVendorId(vendor.getVendorId());
        response.setName(vendor.getName());
        response.setDescription(vendor.getDescription());
        response.setAddress(vendor.getAddress());
        response.setOfficePhone(vendor.getOfficePhone());
        response.setTin(vendor.getTin());
        response.setEmail(vendor.getEmail());
        response.setVendorType(vendor.getVendorType());
        response.setStatus(vendor.getStatus());
        response.setBankDetails(vendor.getBankDetails());
        response.setAttachmentIds(vendor.getAttachmentIds());
        response.setOrganisationId(vendor.getOrganisation().getOrganisationId());
        response.setOrganisationName(vendor.getOrganisation().getOrganisationName());
        response.setCreatedAt(vendor.getCreatedAt());
        response.setUpdatedAt(vendor.getUpdatedAt());
        return response;
    }

    private VendorSummaryResponse mapToVendorSummaryResponse(VendorEntity vendor) {
        VendorSummaryResponse response = new VendorSummaryResponse();
        response.setVendorId(vendor.getVendorId());
        response.setName(vendor.getName());
        response.setVendorType(vendor.getVendorType());
        return response;
    }
}

