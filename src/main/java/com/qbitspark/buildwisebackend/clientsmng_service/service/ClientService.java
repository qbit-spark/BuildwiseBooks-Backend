package com.qbitspark.buildwisebackend.clientsmng_service.service;

import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ProjectResponseForClient;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    ClientResponse createClientWithinOrganisation(UUID organisationId, CreateClientRequest request) throws ItemNotFoundException;

    ClientResponse getClientByIdWithinOrganisation(UUID clientId) throws ItemNotFoundException;

    List<ClientResponse> getAllClientsWithinOrganisation(UUID organisationId) throws ItemNotFoundException;

    Page<ClientResponse> getAllClientsWithinOrganisation(
            UUID organisationId,
            Pageable pageable) throws ItemNotFoundException;

    ClientResponse updateClientWithinOrganisation(UUID clientId, UpdateClientRequest request) throws ItemNotFoundException;

    void deleteClientWithinOrganisation(UUID clientId) throws ItemNotFoundException;

    List<ProjectResponseForClient> getClientProjectsWithinOrganisation(UUID clientId) throws ItemNotFoundException;
}