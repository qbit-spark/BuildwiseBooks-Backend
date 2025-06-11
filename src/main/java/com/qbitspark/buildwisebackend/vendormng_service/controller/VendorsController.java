package com.qbitspark.buildwisebackend.vendormng_service.controller;

import com.qbitspark.buildwisebackend.vendormng_service.payloads.VendorResponse;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.CreateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.ProjectResponseForVendor;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.UpdateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.service.VendorService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorsController {

    private final VendorService vendorService;

    @PostMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createVendor(
            @PathVariable UUID organisationId,
            @RequestBody @Validated CreateVendorRequest request) throws ItemNotFoundException {

        VendorResponse vendor = vendorService.createVendorWithinOrganisation(organisationId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendor created successfully", vendor));
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getVendor(
            @PathVariable UUID vendorId) throws ItemNotFoundException {

        VendorResponse vendor = vendorService.getVendorByIdWithinOrganisation(vendorId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendor retrieved successfully", vendor));
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllVendors(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<VendorResponse> vendors = vendorService.getAllVendorsWithinOrganisation(organisationId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendors retrieved successfully", vendors));
    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateVendor(
            @PathVariable UUID vendorId,
            @RequestBody UpdateVendorRequest request) throws ItemNotFoundException {

        VendorResponse vendor = vendorService.updateVendorWithinOrganisation(vendorId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendor updated successfully", vendor));
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteVendor(
            @PathVariable UUID vendorId) throws ItemNotFoundException {

        vendorService.deleteVendorWithinOrganisation(vendorId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendor deleted successfully"));
    }

//    @GetMapping("/{vendorId}/projects")
//    public ResponseEntity<GlobeSuccessResponseBuilder> getVendorProjects(
//            @PathVariable UUID vendorId) throws ItemNotFoundException {
//
//        List<ProjectResponseForVendor> projects = vendorService.getVendorProjectsWithinOrganisation(vendorId);
//        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Vendor projects retrieved successfully", projects));
//    }
}