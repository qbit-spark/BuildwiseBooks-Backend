package com.qbitspark.buildwisebackend.organisationService.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisationService.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.payloads.CreateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisationService.payloads.OrganisationResponse;
import com.qbitspark.buildwisebackend.organisationService.payloads.UpdateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisationService.service.OrganisationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/organisation")
public class OrganisationController {

    private final OrganisationService organisationService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createOrganisation(@Valid @RequestBody CreateOrganisationRequest request) throws ItemNotFoundException {
        OrganisationEntity organisation = organisationService.createOrganisation(request);
        OrganisationResponse response = mapToResponse(organisation);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationById(@PathVariable UUID id) throws ItemNotFoundException {
        OrganisationEntity organisation = organisationService.getOrganisationById(id);
        OrganisationResponse response = mapToResponse(organisation);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation retrieved successfully", response));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrganisationById(@PathVariable UUID id) throws ItemNotFoundException {
        OrganisationEntity organisation = organisationService.getMyOrganisationById(id);
        OrganisationResponse response = mapToResponse(organisation);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Your organisation retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllOrganisations() {
        List<OrganisationEntity> organisations = organisationService.getAllOrganisations();
        List<OrganisationResponse> responses = organisations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("All organisations retrieved successfully", responses));
    }

    @GetMapping("/my")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllMyOrganisations() throws ItemNotFoundException {
        List<OrganisationEntity> organisations = organisationService.getAllMyOrganisations();
        List<OrganisationResponse> responses = organisations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Your organisations retrieved successfully", responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateOrganisation(@PathVariable UUID id, @Valid @RequestBody UpdateOrganisationRequest request) throws ItemNotFoundException {
        OrganisationEntity organisation = organisationService.updateOrganisation(id, request);
        OrganisationResponse response = mapToResponse(organisation);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation updated successfully", response));
    }

    private OrganisationResponse mapToResponse(OrganisationEntity entity) {
        OrganisationResponse response = new OrganisationResponse();
        response.setOrganisationId(entity.getOrganisationId().toString());
        response.setOrganisationName(entity.getOrganisationName());
        response.setOwnerId(entity.getOwner().getId());
        response.setOwnerUserName(entity.getOwner().getUserName());
        response.setDescription(entity.getOrganisationDescription());
        response.setActive(entity.isActive());
        response.setDeleted(entity.isDeleted());
        response.setCreatedAt(entity.getCreatedDate().toString());
        response.setUpdatedAt(entity.getModifiedDate().toString());
        return response;
    }
}