package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.CreateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.UpdateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.DeductResponse;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service.DeductService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
//@RequestMapping("/api/v1/organisations/{organisationId}/deducts")
//Todo: changed
@RequestMapping("/api/v1/deducts/{organisationId}")
@RequiredArgsConstructor
public class DeductController {

    private final DeductService deductService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createDeduct(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateDeductRequest request) throws ItemNotFoundException, AccessDeniedException {

        DeductResponse response = deductService.createDeduct(organisationId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Deduct created successfully", response));
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllDeducts(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        List<DeductResponse> response = deductService.getAllDeductsByOrganisation(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Deducts retrieved successfully", response));
    }

    @GetMapping("/active")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveDeducts(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        List<DeductResponse> response = deductService.getActiveDeductsByOrganisation(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Active deducts retrieved successfully", response));
    }

    @GetMapping("/{deductId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getDeductById(
            @PathVariable UUID organisationId,
            @PathVariable UUID deductId) throws ItemNotFoundException, AccessDeniedException {

        DeductResponse response = deductService.getDeductById(organisationId, deductId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Deduct details retrieved successfully", response));
    }

    @PutMapping("/{deductId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDeduct(
            @PathVariable UUID organisationId,
            @PathVariable UUID deductId,
            @Valid @RequestBody UpdateDeductRequest request) throws ItemNotFoundException, AccessDeniedException {

        DeductResponse response = deductService.updateDeduct(organisationId, deductId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Deduct updated successfully", response));
    }

    @DeleteMapping("/{deductId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteDeduct(
            @PathVariable UUID organisationId,
            @PathVariable UUID deductId) throws ItemNotFoundException, AccessDeniedException {

        deductService.deleteDeduct(organisationId, deductId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Deduct permanently deleted successfully", null));
    }
}