package com.qbitspark.buildwisebackend.clientsmng_service.service;

import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ProjectResponseForClient;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    ClientResponse createClientWithinOrganisation(UUID organisationId, CreateClientRequest request) throws ItemNotFoundException, AccessDeniedException;

    ClientResponse getClientByIdWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException;

    List<ClientResponse> getAllClientsWithinOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    Page<ClientResponse> getAllClientsWithinOrganisation(
            UUID organisationId,
            Pageable pageable) throws ItemNotFoundException, AccessDeniedException;

    ClientResponse updateClientWithinOrganisation(UUID organisationId, UUID clientId, UpdateClientRequest request) throws ItemNotFoundException, AccessDeniedException;

    void deleteClientWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException;

    List<ProjectResponseForClient> getClientProjectsWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException;
}