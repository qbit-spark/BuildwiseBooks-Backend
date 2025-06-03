package com.qbitspark.buildwisebackend.organisationservice.organisation_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisationservice.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationservice.organisation_mng.payloads.CreateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisationservice.organisation_mng.payloads.UpdateOrganisationRequest;

import java.util.List;
import java.util.UUID;

public interface OrganisationService {
    OrganisationEntity createOrganisation(CreateOrganisationRequest createOrganisationRequest) throws ItemNotFoundException;
    OrganisationEntity getOrganisationById(UUID id) throws ItemNotFoundException;
    List<OrganisationEntity> getAllOrganisations();
    List<OrganisationEntity> getAllMyOrganisations() throws ItemNotFoundException;
    OrganisationEntity updateOrganisation(UUID id, UpdateOrganisationRequest updateOrganisationRequest) throws ItemNotFoundException;
    OrganisationEntity getMyOrganisationById(UUID id) throws ItemNotFoundException;
}
