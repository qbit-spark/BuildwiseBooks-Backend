package com.qbitspark.buildwisebackend.accounting_service.tax_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.CreateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.UpdateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.TaxResponse;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.service.TaxService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisations/{organisationId}/taxes")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createTax(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateTaxRequest request) throws ItemNotFoundException {

        TaxResponse response = taxService.createTax(organisationId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Tax created successfully", response));
    }


    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllTaxes(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<TaxResponse> response = taxService.getAllTaxesByOrganisation(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Tax retrieved successfully", response));
    }


    @GetMapping("/active")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveTaxes(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<TaxResponse> response = taxService.getActiveTaxesByOrganisation(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Active taxes retrieved successfully", response));
    }


    @GetMapping("/{taxId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getTaxById(
            @PathVariable UUID organisationId,
            @PathVariable UUID taxId) throws ItemNotFoundException {

        TaxResponse response = taxService.getTaxById(organisationId, taxId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Tax details retrieved successfully", response));
    }


    @PutMapping("/{taxId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateTax(
            @PathVariable UUID organisationId,
            @PathVariable UUID taxId,
            @Valid @RequestBody UpdateTaxRequest request) throws ItemNotFoundException {

        TaxResponse response = taxService.updateTax(organisationId, taxId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Tax updated successfully", response));
    }


    @DeleteMapping("/{taxId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteTax(
            @PathVariable UUID organisationId,
            @PathVariable UUID taxId) throws ItemNotFoundException {

        taxService.deleteTax(organisationId, taxId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Tax permanently deleted successfully", null));
    }
}